package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SpillListFilterDto {
	private Long financialYearId; // optional
	private Long districtId; // optional
	private Long demandCodeId; // optional
	private String search; // work code/name search, optional
}
