package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.MLCBudgetPostDto;
import com.lsit.dfds.dto.MLCBudgetResponseDto;

public interface MLCBudgetService {

	List<MLCBudgetResponseDto> getMLCBudgetSummary(String username, Long financialYearId);

	MLCBudgetResponseDto saveMLCBudget(MLCBudgetPostDto dto, String createdBy);

}
