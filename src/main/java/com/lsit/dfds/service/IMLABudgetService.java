package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.MLABudgetPostDto;
import com.lsit.dfds.dto.MLABudgetResponseDto;

public interface IMLABudgetService {
//	List<MLCBudgetSummaryDto> getMLABudgetSummary(String username, Long financialYearId);

	MLABudgetResponseDto saveMLABudget(MLABudgetPostDto dto, String username);

	List<MLABudgetResponseDto> getMLABudgetSummary(String username, Long financialYearId);

	List<MLABudgetResponseDto> getAllMLABudgetDetails(String username, Long financialYearId);

}
