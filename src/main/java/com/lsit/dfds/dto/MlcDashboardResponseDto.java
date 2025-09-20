package com.lsit.dfds.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class MlcDashboardResponseDto {
	private long totalWorks;
	private long nodalWorks;
	private long nonNodalWorks;
	private List<WorkStatusCountDto> worksByStatus;

	private Double totalAllocatedBudget;
	private Double totalUtilizedBudget;
	private Double totalRemainingBudget;

	private List<BudgetByFyDto> budgetsByFy;
	private List<WorkProgressItemDto> workProgress;
	private List<FundDemandDetailDto> fundDemands;

	// header cards
	private Double currentFySanctioned;
	private Double currentFyRemaining;
	private Double lastYearSpill;
	private Long totalWorksRecommendedCount;
	private Double totalWorksRecommendedCost;
	private Long totalWorksSanctionedCount;
	private Double totalWorksSanctionedCost;
	private Double permissibleScope;
	private Double totalFundDisbursed;
	// table
	private List<Map<String, Object>> worksTable;

	private double mlcPermissibleScope;
	private String mlcTenurePeriod;
	private String mlcTermStartDate;
	private String mlcTermEndDate;
	private Integer mlcTermNumber;

	public void setMlcPermissibleScope(double mlcPermissibleScope) {
		this.mlcPermissibleScope = mlcPermissibleScope;
	}

	public String getMlcTenurePeriod() {
		return mlcTenurePeriod;
	}

	public void setMlcTenurePeriod(String mlcTenurePeriod) {
		this.mlcTenurePeriod = mlcTenurePeriod;
	}

	public String getMlcTermStartDate() {
		return mlcTermStartDate;
	}

	public void setMlcTermStartDate(String mlcTermStartDate) {
		this.mlcTermStartDate = mlcTermStartDate;
	}

	public String getMlcTermEndDate() {
		return mlcTermEndDate;
	}

	public void setMlcTermEndDate(String mlcTermEndDate) {
		this.mlcTermEndDate = mlcTermEndDate;
	}

	public Integer getMlcTermNumber() {
		return mlcTermNumber;
	}

	public void setMlcTermNumber(Integer mlcTermNumber) {
		this.mlcTermNumber = mlcTermNumber;
	}

}