package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class SpillDecisionDto {
	private Long spillRequestId;
	private boolean approve; // true=approve, false=reject
	private String remarks;
}
