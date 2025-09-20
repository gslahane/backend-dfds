package com.lsit.dfds.dto;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.Data;

@Data
public class MLARecomendWorkResponseDto {
	private Long id;
	private String workName;
	private String workCode;
	private double grossAmount;
	private double balanceAmount;
	private double adminApprovedAmount;
	private WorkStatuses status;
	private String description;

	// ids (optional but kept)
	private Long districtId;
	private Long constituencyId;

	// names (for UI)
	private String districtName;
	private String constituencyName;
	private String schemeName;
	private String implementingAgencyName;
	private String mlaName;

	private String recommendationLetterUrl;

	private Double recommendedAmount;
	private Double districtApprovedAmount;

}
