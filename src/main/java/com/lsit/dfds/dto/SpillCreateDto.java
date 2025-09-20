package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class SpillCreateDto {
	private Long workId;
	private Long toFinancialYearId; // next FY
	private String remarks;
}
