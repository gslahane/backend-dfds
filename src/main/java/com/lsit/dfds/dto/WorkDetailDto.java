// src/main/java/com/lsit/dfds/dto/WorkDetailDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Detailed Work info for the bundle */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkDetailDto {
	private Long workId;
	private String workName;
	private String workCode;

	private String schemeName;
	private String planType; // scheme.baseSchemeType
	private String financialYear; // from scheme.financialYear.finacialYear

	private String districtName;
	private String constituencyName;

	private Double aaAmount; // adminApprovedAmount
	private Double grossWorkOrderAmount; // work.grossAmount
	private Double balanceAmount; // work.balanceAmount

	private LocalDate sanctionDate;
	private String workStatus; // WorkStatuses name if not null

	private Long iaUserId;
	private String iaUserName;
}
