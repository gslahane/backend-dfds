// src/main/java/com/lsit/dfds/dto/ReappropDecisionDto.java
package com.lsit.dfds.dto;

import com.lsit.dfds.enums.ReappropriationStatuses;

import lombok.Data;

@Data
public class ReappropDecisionDto {
	private Long reappropId;
	private ReappropriationStatuses decision; // APPROVED / REJECTED
	private String remarks;
}
