// src/main/java/com/lsit/dfds/service/SchemeFetchServiceImpl.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.SchemeBudgetResponseDto;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchemeFetchServiceImpl implements SchemeFetchService {

	private final SchemesRepository schemesRepository;
	private final SchemeBudgetRepository schemeBudgetRepository;
	private final UserRepository userRepository;

	@Override
	public List<SchemeBudgetResponseDto> fetchSchemesWithFiltersForUser(String username, Long schemeTypeId,
			String planType, Long districtId, Long financialYearId) {

		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long effectiveDistrictId = districtId;
		if (user.getRole() == Roles.DISTRICT_ADMIN || user.getRole() == Roles.IA_ADMIN) {
			if (user.getDistrict() == null) {
				throw new RuntimeException("Logged-in user has no district mapped");
			}
			effectiveDistrictId = user.getDistrict().getId();
		}

		PlanType planTypeEnum = parsePlanType(planType);

		List<Schemes> schemes = (effectiveDistrictId == null)
				? schemesRepository.findFilteredSchemesNoDistrict(financialYearId, planTypeEnum, schemeTypeId)
				: schemesRepository.findFilteredSchemesForDistrict(effectiveDistrictId, financialYearId, planTypeEnum,
						schemeTypeId);

		final Long finalDistrictId = effectiveDistrictId;

		return schemes.stream().map(s -> {
			SchemeBudgetResponseDto dto = new SchemeBudgetResponseDto();
			dto.setSchemeId(s.getId());
			dto.setSchemeName(s.getSchemeName());
			dto.setDescription(s.getDescription());
			dto.setFinancialYear(s.getFinancialYear() != null ? s.getFinancialYear().getFinacialYear() : null);
			dto.setPlanType(s.getPlanType() != null ? s.getPlanType().name() : null);

			// Attach district budget snapshot if we have a district scope + FY
			Long fyId = (s.getFinancialYear() != null ? s.getFinancialYear().getId() : financialYearId);
			if (finalDistrictId != null && fyId != null) {
				SchemeBudget b = schemeBudgetRepository
						.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(finalDistrictId, s.getId(), fyId)
						.orElse(null);
				if (b != null) {
					dto.setDistrictId(b.getDistrict().getId());
					dto.setDistrictName(b.getDistrict().getDistrictName());
					dto.setDistrictAllocatedLimit(b.getAllocatedLimit() != null ? b.getAllocatedLimit() : 0.0);
					dto.setDistrictUtilizedLimit(b.getUtilizedLimit() != null ? b.getUtilizedLimit() : 0.0);
					dto.setDistrictRemainingLimit(b.getRemainingLimit() != null ? b.getRemainingLimit() : 0.0);
				}
			}
			return dto;
		}).toList();
	}

	@Override
	public List<SchemeBudgetResponseDto> fetchSchemesWithFilters(Long schemeTypeId, String planType, Long districtId,
			Long financialYearId) {

		PlanType planTypeEnum = parsePlanType(planType);

		List<Schemes> schemes = (districtId == null)
				? schemesRepository.findFilteredSchemesNoDistrict(financialYearId, planTypeEnum, schemeTypeId)
				: schemesRepository.findFilteredSchemesForDistrict(districtId, financialYearId, planTypeEnum,
						schemeTypeId);

		return schemes.stream().map(s -> {
			SchemeBudgetResponseDto dto = new SchemeBudgetResponseDto();
			dto.setSchemeId(s.getId());
			dto.setSchemeName(s.getSchemeName());
			dto.setDescription(s.getDescription());
			dto.setFinancialYear(s.getFinancialYear() != null ? s.getFinancialYear().getFinacialYear() : null);
			dto.setPlanType(s.getPlanType() != null ? s.getPlanType().name() : null);

			if (districtId != null) {
				Long fyId = (s.getFinancialYear() != null ? s.getFinancialYear().getId() : financialYearId);
				if (fyId != null) {
					schemeBudgetRepository.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(districtId, s.getId(), fyId)
							.ifPresent(b -> {
								dto.setDistrictId(b.getDistrict().getId());
								dto.setDistrictName(b.getDistrict().getDistrictName());
								dto.setDistrictAllocatedLimit(
										b.getAllocatedLimit() != null ? b.getAllocatedLimit() : 0.0);
								dto.setDistrictUtilizedLimit(b.getUtilizedLimit() != null ? b.getUtilizedLimit() : 0.0);
								dto.setDistrictRemainingLimit(
										b.getRemainingLimit() != null ? b.getRemainingLimit() : 0.0);
							});
				}
			}
			return dto;
		}).toList();
	}

	private PlanType parsePlanType(String planType) {
		if (planType == null || planType.isBlank()) {
			return null;
		}
		try {
			return PlanType.valueOf(planType.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Invalid planType: " + planType);
		}
	}
}
