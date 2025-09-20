// src/main/java/com/lsit/dfds/dto/DistrictDemandCountersDto.java
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class DistrictDemandCountersDto {

	@Data
	public static class Block {
		private Double amount; // total amount in the bucket
		private Long count; // number of demands
	}

	// Submitted to State (forwarded by district)
	private Block submittedTotal; // APPROVED_BY_DISTRICT
	private Block approvedByState; // APPROVED_BY_STATE
	private Block rejectedByState; // REJECTED_BY_STATE
	private Block pendingAtState; // APPROVED_BY_DISTRICT but NOT decided yet

	// Received from IA (at district decision stage)
	private Block receivedFromIa; // all from IA (PENDING|SENT_BACK_BY_STATE etc.)
	private Block approvedByDistrict; // APPROVED_BY_DISTRICT
	private Block rejectedByDistrict; // REJECTED_BY_DISTRICT
	private Block pendingAtDistrict; // PENDING or SENT_BACK_BY_STATE
}
