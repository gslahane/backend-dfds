package com.lsit.dfds.dto;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.Data;

@Data
public class MLCRecomendWorkResponseDto {
	private Long id;
	private String workName;
	private String workCode;
	private double grossAmount;
	private double balanceAmount;
	private WorkStatuses status;
	private String description;

	private Long districtId;
	private String districtName;

	private String schemeName;
	private Boolean isNodalWork;
	private String mlcName;

	private String recommendationLetterUrl;

	private Double recommendedAmount;
	private Double districtApprovedAmount;

}
