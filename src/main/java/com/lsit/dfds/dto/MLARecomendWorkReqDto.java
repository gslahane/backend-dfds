// com.lsit.dfds.dto.MLARecomendWorkReqDto
package com.lsit.dfds.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MLARecomendWorkReqDto {

	private Long mlaid; // optional fallback if user->MLA mapping is missing
	private Long financialYearId; // keep for spillover
	private Long sectorID; // keep if you use it
	private Long implementingAgencyUserId; // optional IA to set at recommendation time
	private String workCode;
	@NotBlank
	private String workName;

	@Positive
	private double recommendedAmount; // MLA-proposed amount

	private String description;
	private List<TaxDetailDto> taxes; // optional, store for audit; don't compute work portions yet
}
