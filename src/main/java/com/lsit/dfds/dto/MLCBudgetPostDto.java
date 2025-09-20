package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MLCBudgetPostDto {
	private Long financialYearId;
	private Long mlcId;
	// private Long schemeId; // ✅ Added Scheme
	private Double allocatedLimit;
	private String remarks;
}
