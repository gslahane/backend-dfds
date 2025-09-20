// src/main/java/com/lsit/dfds/dto/FundDemandStatusUpdateDto.java
package com.lsit.dfds.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.DemandStatuses;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FundDemandStatusUpdateDto {

	// OPTION A: single demand
	private Long fundDemandId;

	// OPTION B: bulk specific demands
	private List<Long> fundDemandIds;

	// OPTION C: approve all eligible (see service logic for eligibility)
	private Boolean approveAll; // default false

	@NotNull(message = "newStatus is required")
	private DemandStatuses newStatus;

	private String remarks;
}
