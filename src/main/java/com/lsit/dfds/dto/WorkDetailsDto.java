package com.lsit.dfds.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkDetailsDto {
	private Long workId;
	private String workName;
	private String workCode;
	private String planType;
	private String schemeName;
	private String districtName;
	private String constituencyName;
	private String mlaName;
	private String mlcName;
	private Double adminApprovedAmount;
	private Double allocatedBudget;
	private Double utilizedBudget;
	private Double remainingBudget;
	private Double pendingApprovalBudget;
	private Double progressPercentage;
	private List<FundDemandDto> fundDemands;
}
