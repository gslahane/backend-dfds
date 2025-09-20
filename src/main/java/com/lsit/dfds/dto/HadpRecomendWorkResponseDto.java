package com.lsit.dfds.dto;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.Data;

@Data
public class HadpRecomendWorkResponseDto {
	private Long id;
	private String workName;
	private double grossAmount;
	private double balanceAmount;
	private WorkStatuses status;
	private String description;
	private Long financialYearId;
	private Long districtId;
	private String districtName;

	private Long talukaId; // echoed if provided
	private String talukaName; // resolved from repo

	private String schemeName;

	private String recommendationLetterUrl;
	private Double recommendedAmount;
	private Double districtApprovedAmount;

}
