package com.lsit.dfds.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DAPBudgetResponceDto {

	private String district; // Name of the district
	private String plan; // The Plan (e.g., DAP, etc.)
	private String demandCode; // Code related to the demand
	private Double allocated; // The allocated amount
	private Double balance; // The balance amount

}
