// src/main/java/com/lsit/dfds/dto/MlcSummaryRowDto.java
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MlcSummaryRowDto {
	private Long mlcId;
	private String mlcName;
	private Double budgetedAmount;
	private Double fundsDisbursed;
	private Double balanceFunds;
	private Long pendingDemands;
	private Long nodalWorks; // optional quick stat
	private Long nonNodalWorks; // optional quick stat
}
