package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchemeRespDto {

	private Long id;
	private String schemeName;
	private String schemeCode;
	private String crcCode;
	private String objectCode;
	private String objectName;
	private String majorHeadName;
	private String description;
	private Double allocatedBudget;
	private Double estimatedBudget;
	private Double revisedBudget;
	private Double nextBudget;
	private Long financialYearId;
	private String financialYearName; // optional
	private Long schemeTypeId;
	private String schemeTypeName; // optional
	private PlanType planType;
	private String createdBy;
	private String createdAt; // or use LocalDateTime if preferred
	private Long sectorId;
	private String sectorName; // optional
}
