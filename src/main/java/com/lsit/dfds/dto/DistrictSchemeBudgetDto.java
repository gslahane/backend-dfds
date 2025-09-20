package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class DistrictSchemeBudgetDto {
	private Long districtId;
	private Long schemeId;
	private Long schemeTypeId;
	private Long financialYearId;
	private Double allocatedLimit;
	private String remarks;
}
