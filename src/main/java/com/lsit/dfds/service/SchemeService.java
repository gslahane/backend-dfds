package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.SchemeRespDto;
import com.lsit.dfds.dto.SchemerequestDto;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.enums.BaseSchemeType;
import com.lsit.dfds.repo.SchemesRepository;

@Service
public class SchemeService implements ISchemeService {
	@Autowired
	private SchemesRepository schemesRepository;

	@Override
	public List<SchemeRespDto> getAllScheme(String username, SchemerequestDto request) {

		List<Schemes> schemes;

		// Case 1: Both Financial Year and District selected — Priority case
		if (request.getFinancialId() != null && request.getDistrictId() != null) {
			List<Schemes> districtSchemes = schemesRepository.findByDistricts_Id(request.getDistrictId());

			schemes = districtSchemes.stream().filter(s -> request.getFinancialId() == null
					|| (s.getFinancialYear() != null && s.getFinancialYear().getId().equals(request.getFinancialId())))
					.filter(s -> request.getPlanType() == null || s.getPlanType() == request.getPlanType())
					.filter(s -> request.getSchemeTypeName() == null || (s.getSchemeType() != null && BaseSchemeType
							.valueOf(request.getSchemeTypeName().toUpperCase()) == s.getSchemeType().getBaseType()))
					.collect(Collectors.toList());

		} else {
			// Case 2: Any other combination — use custom query in repository
			schemes = schemesRepository.findFilteredSchemesByName(request.getFinancialId(), request.getPlanType(),
					request.getDistrictId(), request.getSchemeTypeName());
		}

		return schemes.stream().map(this::mapToDto).collect(Collectors.toList());
	}

	private SchemeRespDto mapToDto(Schemes scheme) {
		SchemeRespDto dto = new SchemeRespDto();
		dto.setId(scheme.getId());
		dto.setSchemeName(scheme.getSchemeName());
		dto.setSchemeCode(scheme.getSchemeCode());
		dto.setCrcCode(scheme.getCrcCode());
		dto.setObjectCode(scheme.getObjectCode());
		dto.setObjectName(scheme.getObjectName());
		dto.setMajorHeadName(scheme.getMajorHeadName());
		dto.setDescription(scheme.getDescription());
		dto.setAllocatedBudget(scheme.getAllocatedBudget());
		dto.setEstimatedBudget(scheme.getEstimatedBudget());
		dto.setRevisedBudget(scheme.getRevisedBudget());
		dto.setNextBudget(scheme.getNextBudget());
		dto.setPlanType(scheme.getPlanType());
		dto.setCreatedBy(scheme.getCreatedBy());

		if (scheme.getCreatedAt() != null) {
			dto.setCreatedAt(scheme.getCreatedAt().toString());
		}

		if (scheme.getFinancialYear() != null) {
			dto.setFinancialYearId(scheme.getFinancialYear().getId());
		}

		if (scheme.getSchemeType() != null) {
			dto.setSchemeTypeId(scheme.getSchemeType().getId());
		}

		if (scheme.getSector() != null) {
			dto.setSectorId(scheme.getSector().getId());
		}

		return dto;
	}

}
