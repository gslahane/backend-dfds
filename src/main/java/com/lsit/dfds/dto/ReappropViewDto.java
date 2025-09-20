// src/main/java/com/lsit/dfds/dto/ReappropViewDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReappropViewDto {
	private Long id;
	private Long districtId;
	private String districtName;
	private Long financialYearId;
	private String financialYear;
	private Long demandCodeId;
	private String demandCodeName;
	private Long targetSchemeId;
	private String targetSchemeName;
	private Double totalTransferAmount;
	private String status; // PENDING/APPROVED/REJECTED
	private LocalDate date;
	private String remarks;

	// sources detail for view
	private List<ReappropViewSourceItem> sources;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ReappropViewSourceItem {
		private Long sourceSchemeId;
		private String sourceSchemeName;
		private Double transferAmount;
	}
}
