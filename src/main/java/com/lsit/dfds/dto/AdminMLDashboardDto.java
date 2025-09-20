package com.lsit.dfds.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminMLDashboardDto {

	private String nodalDistrict; // Nodal District
	private String type; // Type (e.g., MLA or MLC)
	private String assemblyConstituency; // Assembly Constituency
	private int term; // Term
	private String mlaMlcName; // MLA/MLC Name
	private Double budgetedAmount; // Budgeted Amount
	private Double fundsDisbursed; // Funds Disbursed
	private Double balanceFunds; // Balance Funds
	private Double pendingDemandAmount;
}