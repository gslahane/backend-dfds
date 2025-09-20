package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class FundDemandFilterRequest {
	private Long financialYearId;
	private Long schemeTypeId;
	private Long schemeId;
	private Long workId;
	private Long mlaId;
	private Long mlcId;
	private Long districtId;
	private String demandStatus; // PENDING, APPROVED_BY_STATE, etc.
}
