package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class HADPRecommendWorkReqDto {

	private Long hadpTalukaId; // Required: HADP taluka for the recommendation
	private Long financialYearId; // Required: FY for this recommendation

	private String workName; // Work title
	private String description; // Work description
	private Double recommendedAmount; // Recommended amount

	private List<TaxDetailDto> taxes; // Optional: Tax lines, if any

	// Note: schemeId removed completely, backend will fetch scheme using
	// PlanType.HADP
}
