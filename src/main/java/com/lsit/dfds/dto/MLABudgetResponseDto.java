package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MLABudgetResponseDto {
	private Long mlaId; // MLA identifier
	private String mlaName; // MLA name
	private String district; // District name (assuming from Constituency)
	private Long constituencyId; // Constituency identifier
	private String constituencyName; // Constituency name

	private Long schemeId; // Scheme identifier (optional)
	private String schemeName; // Scheme name (optional)

	private String financialYear; // Financial Year
	private Double allocatedLimit; // Allocated limit (from budget)
	private Double utilizedLimit; // Utilized limit (from budget)
	private String status; // Status (ACTIVE, INACTIVE, etc.)
}
