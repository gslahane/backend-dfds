package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.DAPBudgetResponceDto;
import com.lsit.dfds.dto.DAPDistrictBudgetPostDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.DistrictLimitAllocation;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FinancialYearBudget;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictLimitAllocationRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearBudgetRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistrictBudgetService implements IDistrictBudgetService {

	private final UserRepository userRepository;
	private final DistrictLimitAllocationRepository districtLimitAllocationRepository;
	private final FinancialYearBudgetRepository financialYearBudgetRepository;
	private final DemandCodeRepository demandCodeRepository;
	private final FinancialYearRepository financialYearRepository;
	private final DistrictRepository districtRepository;

	@Override
	public List<DAPBudgetResponceDto> getDAPBudget(String username, Long financialYearId) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		List<DistrictLimitAllocation> allocations = districtLimitAllocationRepository
				.findAllByFinancialYear_IdAndPlanType(financialYearId, PlanType.DAP);

		List<DAPBudgetResponceDto> response = new ArrayList<>();
		for (DistrictLimitAllocation a : allocations) {
			DAPBudgetResponceDto dto = new DAPBudgetResponceDto();
			dto.setDistrict(a.getDistrict().getDistrictName());
			dto.setPlan(PlanType.DAP.name());
			dto.setDemandCode(a.getSchemeType() != null ? a.getSchemeType().getSchemeType() : "-");
			dto.setAllocated(a.getAllocatedLimit());
			dto.setBalance(a.getRemainingLimit());
			response.add(dto);
		}
		return response;
	}

	@Override
	@Transactional
	public DistrictLimitAllocation saveDAPDistrictBudget(DAPDistrictBudgetPostDto dto, String username) {
		// Validate user roles
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		if (!(role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)) {
			throw new RuntimeException("Unauthorized user: " + username);
		}

		// Validate plan type
		if (dto.getPlanType() == null || dto.getPlanType() != PlanType.DAP) {
			throw new RuntimeException("PlanType must be DAP for DAP district budget allocation");
		}

		// DemandCode is mandatory for DAP bucket
		if (dto.getSchemeTypeId() == null) {
			throw new RuntimeException("schemeTypeId (DemandCode) is required for DAP allocation");
		}

		// Fetch related
		District district = districtRepository.findById(dto.getDistrictId())
				.orElseThrow(() -> new RuntimeException("District not found"));
		FinancialYear financialYear = financialYearRepository.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));
		DemandCode demandCode = demandCodeRepository.findById(dto.getSchemeTypeId())
				.orElseThrow(() -> new RuntimeException("Demand Code not found"));

		// Upsert on unique key (district, fy, DAP, demandCode)
		DistrictLimitAllocation allocation = districtLimitAllocationRepository
				.findByDistrict_IdAndFinancialYear_IdAndPlanTypeAndSchemeType_Id(district.getId(),
						financialYear.getId(), PlanType.DAP, demandCode.getId())
				.orElseGet(DistrictLimitAllocation::new);

		allocation.setDistrict(district);
		allocation.setFinancialYear(financialYear);
		allocation.setPlanType(PlanType.DAP);
		allocation.setSchemeType(demandCode);

		// If updating, preserve utilized; recompute remaining = new allocated -
		// utilized
		Double utilized = allocation.getUtilizedLimit() == null ? 0.0 : allocation.getUtilizedLimit();
		allocation.setAllocatedLimit(dto.getAllocatedLimit());
		allocation.setUtilizedLimit(utilized);
		allocation.setRemainingLimit(dto.getAllocatedLimit() - utilized);
		allocation.setStatus(Statuses.ACTIVE);
//		allocation.setRemarks(dto.getRemarks());
		allocation.setCreatedBy(username);

		DistrictLimitAllocation saved = districtLimitAllocationRepository.save(allocation);

		// Optional: Update FY budget utilized (your earlier behavior).
		// If this should be "state-level aggregate utilization", keep it. Otherwise,
		// remove.
		FinancialYearBudget fyBudget = financialYearBudgetRepository
				.findByFinancialYear_IdAndPlanType(financialYear.getId(), PlanType.DAP)
				.orElseThrow(() -> new RuntimeException("FinancialYearBudget not found for DAP"));
		fyBudget.setUtilizedLimit(fyBudget.getUtilizedLimit() + dto.getAllocatedLimit());
		financialYearBudgetRepository.save(fyBudget);

		return saved;
	}
}
