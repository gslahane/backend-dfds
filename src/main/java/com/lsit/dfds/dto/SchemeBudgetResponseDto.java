package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class SchemeBudgetResponseDto {
	private Long schemeId;
	private String schemeName;
	private String planType;
	private String description;
	private Double allocatedBudget;
	private Double estimatedBudget;
	private Double revisedBudget;
	private Double nextBudget;

	// District-level details
	private Long districtId;
	private String districtName;
	private Double districtAllocatedLimit;
	private Double districtUtilizedLimit;
	private Double districtRemainingLimit;

	private String financialYear;
	private String schemeTypeName;
}
