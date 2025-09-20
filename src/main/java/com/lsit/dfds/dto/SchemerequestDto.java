package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SchemerequestDto {
	private Long financialId; // from "All Years"
	private PlanType planType; // from "All Plan Types"
	private Long districtId; // from "All Districts"
	private String schemeTypeName; // from "All Scheme Types"
}
