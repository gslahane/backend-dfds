// src/main/java/com/lsit/dfds/dto/MlaSummaryRowDto.java
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MlaSummaryRowDto {
	private Long mlaId;
	private String mlaName;
	private String constituencyName;
	private Double budgetedAmount; // MLABudget sum for MLA (FY)
	private Double fundsDisbursed; // sum TRANSFERRED for MLA (FY & district)
	private Double balanceFunds; // budget - disbursed
	private Long pendingDemands; // count of pending for MLA (district scope)
}
