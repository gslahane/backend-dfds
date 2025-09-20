// src/main/java/com/lsit/dfds/dto/HadpDashboardResponseDto.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HadpDashboardResponseDto {
	private long totalWorks;

	private List<WorkStatusCountDto> worksByStatus;

	private Double totalAllocatedBudget;
	private Double totalUtilizedBudget;
	private Double totalRemainingBudget;

	private List<BudgetByFyDto> budgetsByFy;

	private List<WorkProgressItemDto> workProgress;

	private List<FundDemandDetailDto> fundDemands;
}
