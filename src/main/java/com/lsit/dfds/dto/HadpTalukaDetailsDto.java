// src/main/java/com/lsit/dfds/dto/HadpTalukaDetailsDto.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HadpTalukaDetailsDto {
	// HADP row
	private Long hadpId;
	private String hadpStatus; // ACTIVE/CLOSED
	private String hadpType; // GROUP / SUB_GROUP

	// Location & scheme context
	private Long districtId;
	private String districtName;
	private Long talukaId;
	private String talukaName;
	private Long schemeId;
	private String schemeName;
	private Long financialYearId;
	private String financialYear;

	// Budget (summed for that HADP row)
	private Double allocatedLimit;
	private Double utilizedLimit;
	private Double remainingLimit;

	// Works
	private Integer totalWorks;
	private List<HadpWorkSummaryDto> works;
}
