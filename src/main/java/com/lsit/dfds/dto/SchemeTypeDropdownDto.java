package com.lsit.dfds.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemeTypeDropdownDto {
	private Long id; // schemeTypeId
	private String schemeType; // name
	private String baseSchemeType; // enum value (Revenue, Capital, Debt)
	private Long financialYearId; // linked financial year
	private List<Long> districtIds; // list of district IDs
}
