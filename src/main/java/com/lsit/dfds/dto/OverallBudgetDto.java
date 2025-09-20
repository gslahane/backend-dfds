package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsit.dfds.enums.PlanType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OverallBudgetDto {

	private PlanType planType;

	private Double allocatedLimit = 0.0; // Allocated by State Admin
	private Double utilizedLimit = 0.0;

	// Calculated field: remainingLimit is automatically calculated based on
	// allocated and utilized limits
	@JsonProperty("remainingLimit") // Ensure it appears in the response
	public Double getRemainingLimit() {
		return allocatedLimit - utilizedLimit;
	}
}