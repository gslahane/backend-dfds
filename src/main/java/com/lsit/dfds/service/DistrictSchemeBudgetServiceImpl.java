package com.lsit.dfds.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.DistrictSchemeBudgetDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.DistrictLimitAllocation;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictLimitAllocationRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistrictSchemeBudgetServiceImpl implements DistrictSchemeBudgetService {

	private final DistrictRepository districtRepository;
	private final SchemesRepository schemesRepository;
	private final DemandCodeRepository schemeTypeRepository;
	private final FinancialYearRepository financialYearRepository;
	private final SchemeBudgetRepository schemeBudgetRepository;
	private final DistrictLimitAllocationRepository districtLimitAllocationRepository;

	@Override
	@Transactional
	public String allocateBudgetToScheme(DistrictSchemeBudgetDto dto, String createdBy) {

		// 0) Normalize amount
		final double amount = dto.getAllocatedLimit() == null ? 0.0 : dto.getAllocatedLimit();
		if (amount <= 0.0) {
			throw new RuntimeException("Allocate amount must be > 0");
		}

		// 1) Load core refs
		District district = districtRepository.findById(dto.getDistrictId())
				.orElseThrow(() -> new RuntimeException("District not found"));

		Schemes scheme = schemesRepository.findById(dto.getSchemeId())
				.orElseThrow(() -> new RuntimeException("Scheme not found"));

		DemandCode demandCode = schemeTypeRepository.findById(dto.getSchemeTypeId())
				.orElseThrow(() -> new RuntimeException("Demand Code (scheme type) not found"));

		FinancialYear financialYear = financialYearRepository.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));

		// 2) Guardrails: DAP only + FY + DemandCode membership
		if (scheme.getPlanType() == null || scheme.getPlanType() != PlanType.DAP) {
			throw new RuntimeException("Scheme is not a DAP scheme");
		}
		if (scheme.getFinancialYear() == null || !scheme.getFinancialYear().getId().equals(dto.getFinancialYearId())) {
			throw new RuntimeException("Scheme and selected Financial Year mismatch");
		}
		if (scheme.getSchemeType() == null || !scheme.getSchemeType().getId().equals(dto.getSchemeTypeId())) {
			throw new RuntimeException("Selected scheme does not belong to the chosen Demand Code");
		}
		// Optional: ensure DemandCode also belongs to same FY (good hygiene)
		if (!demandCode.getFinancialYear().getId().equals(dto.getFinancialYearId())) {
			throw new RuntimeException("Demand Code and selected Financial Year mismatch");
		}

		// 3) Get district's DAP bucket for this DemandCode
		DistrictLimitAllocation limit = districtLimitAllocationRepository
				.findByDistrict_IdAndFinancialYear_IdAndPlanTypeAndSchemeType_Id(dto.getDistrictId(),
						dto.getFinancialYearId(), PlanType.DAP, // ✅ force DAP bucket
						dto.getSchemeTypeId())
				.orElseThrow(() -> new RuntimeException("District DAP allocation for this Demand Code not found"));

		if (limit.getRemainingLimit() < amount) {
			throw new RuntimeException("Insufficient remaining limit in this Demand Code bucket");
		}

		// 4) Fetch existing SchemeBudget row (created during mapping)
		SchemeBudget schemeBudget = schemeBudgetRepository
				.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(dto.getDistrictId(), dto.getSchemeId(),
						dto.getFinancialYearId())
				.orElseThrow(
						() -> new RuntimeException("SchemeBudget not found. Please map scheme to district first."));

		// 5) Apply allocation to SchemeBudget (INCREMENT, not overwrite)
		double prevAllocated = schemeBudget.getAllocatedLimit() == null ? 0.0 : schemeBudget.getAllocatedLimit();
		double prevUtilized = schemeBudget.getUtilizedLimit() == null ? 0.0 : schemeBudget.getUtilizedLimit();
		double newAllocated = prevAllocated + amount;

		schemeBudget.setAllocatedLimit(newAllocated);
		schemeBudget.setRemainingLimit(newAllocated - prevUtilized);
		schemeBudget.setSchemeType(demandCode);
		schemeBudget.setRemarks(dto.getRemarks());
		schemeBudget.setCreatedBy(createdBy);
		schemeBudgetRepository.save(schemeBudget);

		// 6) Deduct from the district's DemandCode bucket
		limit.setUtilizedLimit(limit.getUtilizedLimit() + amount);
		limit.setRemainingLimit(limit.getAllocatedLimit() - limit.getUtilizedLimit());
		districtLimitAllocationRepository.save(limit);

		return "✅ Allocated ₹" + amount + " to scheme '" + scheme.getSchemeName() + "' from DAP Demand Code '"
				+ demandCode.getSchemeType() + "'.";
	}

}
