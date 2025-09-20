// WorkProgressItemDto.java
package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // hides 'nodal' when null (MLA case)
public class WorkProgressItemDto {
	private Long workId;
	private String workName;
	private String workCode;
	private Double adminApprovedAmount;
	private Double utilizedFromDemands; // sum of approved FundDemand.netPayable
	private Double progressPercent; // utilized / adminApprovedAmount * 100
	private String status;
	private Boolean nodal; // only used for MLC

	// 7-arg ctor (MLA)
	public WorkProgressItemDto(Long workId, String workName, String workCode, Double adminApprovedAmount,
			Double utilizedFromDemands, Double progressPercent, String status) {
		this.workId = workId;
		this.workName = workName;
		this.workCode = workCode;
		this.adminApprovedAmount = adminApprovedAmount;
		this.utilizedFromDemands = utilizedFromDemands;
		this.progressPercent = progressPercent;
		this.status = status;
		this.nodal = null; // not applicable for MLA
	}

	// 8-arg ctor (MLC)
	public WorkProgressItemDto(Long workId, String workName, String workCode, Double adminApprovedAmount,
			Double utilizedFromDemands, Double progressPercent, String status, Boolean nodal) {
		this(workId, workName, workCode, adminApprovedAmount, utilizedFromDemands, progressPercent, status);
		this.nodal = nodal;
	}
}
