package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MLCBudgetResponseDto {
	private Long id;
	private String financialYear;
	private String mlcName;
	private String disrict;
	private String category;
	private String schemeName; // ✅ Added
	private Double allocatedLimit;
	private Double utilizedLimit;
	private Double remainingLimit;
	private String status;
	private String remarks;
}
