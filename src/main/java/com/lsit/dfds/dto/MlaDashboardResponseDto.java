// MlaDashboardResponseDto.java
package com.lsit.dfds.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MlaDashboardResponseDto {
	// Summary
	private Long totalWorks;
	private List<WorkStatusCountDto> worksByStatus;
	private Double totalAllocatedBudget;
	private Double totalUtilizedBudget;
	private Double totalRemainingBudget;

	// Breakdowns
	private List<BudgetByFyDto> budgetsByFy;
	private List<WorkProgressItemDto> workProgress;

	// Demands table
	private List<FundDemandDetailDto> fundDemands;

	// header cards
	private Double currentFySanctioned; // allocated for selected FY
	private Double currentFyRemaining; // allocated - utilized (selected FY)
	private Double lastYearSpill; // sum(carryForwardIn) for selected FY budgets
	private Long totalWorksRecommendedCount;
	private Double totalWorksRecommendedCost; // sum(grossAmount; fallback adminApprovedAmount)
	private Long totalWorksSanctionedCount; // AA_APPROVED or later
	private Double totalWorksSanctionedCost; // sum(adminApprovedAmount of AA+)
	private Double permissibleScope; // equals currentFyRemaining (you can refine later)
	// optional overall disbursal metric shown in table header
	private Double totalFundDisbursed; // sum of TRANSFERRED demands (netPayable)
	// work table (simple map so you don’t need a new DTO)
	private List<Map<String, Object>> worksTable;

	// MLA term details
	private String mlaTermStartDate; // Start date of MLA term (e.g., yyyy-MM-dd)
	private String mlaTermEndDate; // End date of MLA term (e.g., yyyy-MM-dd)
	private Integer mlaTermNumber; // MLA term number
	private Double mlaPermissibleScope; // Permissible scope for MLA term
	private String mlaTenurePeriod; // MLA tenure period (e.g., "2019-2024" or "5 years")
}