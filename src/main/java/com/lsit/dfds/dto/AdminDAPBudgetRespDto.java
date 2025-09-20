package com.lsit.dfds.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminDAPBudgetRespDto {
	private Double totalBudget;
	private Double fundsDistributed;
	private Double remainingBalance;
	private Double pendingDemandAmount;

}
