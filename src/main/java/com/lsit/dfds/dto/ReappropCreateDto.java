// src/main/java/com/lsit/dfds/dto/ReappropCreateDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ReappropCreateDto {
	private Long financialYearId;
	private Long demandCodeId; // enforce same Demand Code
	private Long targetSchemeId; // scheme within that Demand Code
	private List<SourceItem> sources; // multiple sources
	private String remarks;
	private LocalDate date; // optional, else use today

	@Data
	public static class SourceItem {
		private Long schemeId;
		private Double amount;
	}
}
