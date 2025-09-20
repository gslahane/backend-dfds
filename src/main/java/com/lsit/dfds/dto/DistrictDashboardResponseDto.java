// src/main/java/com/lsit/dfds/dto/DistrictDashboardResponseDto.java
package com.lsit.dfds.dto;

import java.util.List;
import java.util.Map;

import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class DistrictDashboardResponseDto {
	// echo filters
	private Long financialYearId;
	private PlanType planType;
	private Long districtId;
	private String districtName;

	// Top cards (per plan). Example keys: "MLA","MLC","HADP","DAP"
	private Map<String, DistrictOverviewDto> overview;

	// “MLA Details — <District>” (table)
	private List<MlaSummaryRowDto> mlaRows;

	// Optional: MLC summary table
	private List<MlcSummaryRowDto> mlcRows;

	// IA Demands summary (second screenshot section)
	private DistrictDemandCountersDto demandsSummary;

	// Flat table for FE (district view of demands)
	private List<FundDemandDetailDto> demands;
}
