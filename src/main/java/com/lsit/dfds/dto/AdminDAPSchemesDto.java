package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDAPSchemesDto {
	private String scheme;
	private double allocatedLimit;
	private double PendingAmount;
	private double utilizedLimit;

	@JsonProperty("remainingLimit") // Ensure it appears in the response
	public Double getRemainingLimit() {
		return allocatedLimit - utilizedLimit;
	}
}
