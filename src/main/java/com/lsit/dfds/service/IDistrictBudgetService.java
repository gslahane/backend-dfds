package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.DAPBudgetResponceDto;
import com.lsit.dfds.dto.DAPDistrictBudgetPostDto;
import com.lsit.dfds.entity.DistrictLimitAllocation;

public interface IDistrictBudgetService {

	List<DAPBudgetResponceDto> getDAPBudget(String username, Long financialYear_Id);

	DistrictLimitAllocation saveDAPDistrictBudget(DAPDistrictBudgetPostDto dto, String username);

}
