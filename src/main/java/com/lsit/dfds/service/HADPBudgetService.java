package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.HADPBudgetAllocationDto;
import com.lsit.dfds.dto.HADPBudgetResponseDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.enums.HadpType;

public interface HADPBudgetService {
	String allocateBudget(HADPBudgetAllocationDto dto, String createdBy);

	List<HADPBudgetResponseDto> fetchBudgets(String username, Long districtId, Long talukaId, Long financialYearId);

	List<IdNameDto> getDistrictsByHadpPlanType(HadpType planType);

	List<IdNameDto> getTalukasByDistrictId(Long districtId);

	List<IdNameDto> getTalukasByHadpType(HadpType planType);
}
