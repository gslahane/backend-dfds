package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.Data;

@Data
public class WorkByResponseDto {

	private Long id;
	private String workCode;
	private String workName;
	private Double adminApprovedAmount;
	private String schemeName;
	private String districtName;
	private String iaName;
	private String mlaName;
	private String mlcName;
	private WorkStatuses status;
	private LocalDate sanctionDate;
	private LocalDateTime createdAt;
	private Double recommendedAmount;
	private String recommendationLetterUrl;
	private String constituencyName;
	private String adminApprovedLetterUrl;
}
