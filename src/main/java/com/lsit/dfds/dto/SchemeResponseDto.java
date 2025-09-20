package com.lsit.dfds.dto;

import com.lsit.dfds.enums.BaseSchemeType;
import com.lsit.dfds.enums.PlanType;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemeResponseDto {
	private Long id;
	private String schemeName;
	private String schemeCode;
	private String description;
	private BaseSchemeType baseSchemeType;
	private PlanType planType;
	private String financialYearName;
	private String sectorName;
	private String createdBy;
}
