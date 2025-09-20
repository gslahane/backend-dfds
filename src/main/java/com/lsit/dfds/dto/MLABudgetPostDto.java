package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MLABudgetPostDto {
	private Long financialYearId;
	private Long mlaId;
	private Long constituencyId;
	private Double allocatedLimit;
	private String remarks;

	// private Long schemeId; // ✅ Optional scheme mapping

}
