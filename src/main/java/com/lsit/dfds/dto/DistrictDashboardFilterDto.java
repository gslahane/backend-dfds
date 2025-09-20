// src/main/java/com/lsit/dfds/dto/DistrictDashboardFilterDto.java
package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class DistrictDashboardFilterDto {
	private Long financialYearId; // required for precise numbers (default to current FY on FE)
	private PlanType planType; // MLA/MLC/HADP/DAP (null => All)
	private Long mlaId; // optional, when planType = MLA
	private Long mlcId; // optional, when planType = MLC
	private String search; // free text (work title, AA letter, etc.)
}
