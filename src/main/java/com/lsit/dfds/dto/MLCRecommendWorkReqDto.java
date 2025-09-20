// src/main/java/com/lsit/dfds/dto/MLCRecommendWorkReqDto.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class MLCRecommendWorkReqDto {
	private String workName; // required
	private String workCode;
	private Long sectorId;
	private String description; // optional
	private Double recommendedAmount; // required
	private Long implementingAgencyUserId;
	private Long targetDistrictId; // required (MLC can recommend in any district)
//	private Long schemeId; // optional (use MLC defaultScheme if null)
	private Long financialYearId;
	private List<TaxDetailDto> taxes; // optional (frontend-calculated tax lines)
}
