package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class WorkListFilter {
	private Long districtId; // Filter by district
	private Long constituencyId; // Filter by constituency
	private Long talukaId; // Filter by taluka (if applicable)
	private Long financialYearId; // Filter by financial year
	private PlanType planType; // Filter by plan type (DAP, MLA, MLC, HADP)
	private String search; // Search by work name or code
	private String schemeId; // Filter by scheme ID
}
