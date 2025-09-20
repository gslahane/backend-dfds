package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class HADPBudgetResponseDto {
	private Long id;
	private Long hadpId;
	private Long schemeId;
	private String schemeName;
	private Long districtId;
	private String districtName;
	private Long talukaId;
	private String talukaName;
	private String financialYear;
	private Double allocatedLimit;
	private Double utilizedLimit;
	private Double remainingLimit;
	private String status;
	private String remarks;
}
