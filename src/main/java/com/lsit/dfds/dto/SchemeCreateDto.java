package com.lsit.dfds.dto;

import com.lsit.dfds.enums.BaseSchemeType;
import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class SchemeCreateDto {

	private String schemeName;
	private String schemeCode;
	private String crcCode;
	private String objectCode;
	private String objectName;
	private String majorHeadName;
	private String description;

	private Long financialYearId; // Reference to FinancialYear
	private BaseSchemeType baseSchemeType; // ✅ Enum instead of SchemeType ID
	private Long sectorId; // Reference to Sector

	private PlanType planType; // Enum: DAP, MLA, MLC, HADP
}
