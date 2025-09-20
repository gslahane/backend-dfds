package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDAPDashboardResponce {
	private DemandCode schemeType;
	private District district;
	private Double allocatedLimit;
	private Double utilizedLimit;

	@JsonProperty("remainingLimit") // Ensure it appears in the response
	public Double getRemainingLimit() {
		return allocatedLimit - utilizedLimit;
	}

	private Double pendingDemand;

}
