package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DAPDistrictBudgetPostDto {
	private Long districtId;
	private Long schemeTypeId; // optional
	private Long financialYearId;
	private Double allocatedLimit;
	private PlanType planType;

}
