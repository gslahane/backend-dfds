package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class AdminDAPDashboardRequest {
	private Long financialYearId;
	private Long districtId;
	private Long schemeTypeId; // assuming this is an enum
}