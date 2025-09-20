package com.lsit.dfds.dto;

import java.time.LocalDate;

import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

import lombok.Data;

@Data
public class WorkDto {
	private Long id;
	private String workName;
	private String workCode;
	private Double grossAmount;
	private Double balanceAmount;
	private Double adminApprovedAmount;
	private WorkStatuses status;
	private LocalDate workStartDate;
	private LocalDate workEndDate;
	private String description;
	private Long districtId;
	private Long constituencyId;
	private PlanType planType;
	private Long financialYearId;
	private String schemeName;
	private String implementingAgencyName;
	private String recommendationLetterUrl;
}
