package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class DistrictSchemeMappingDto {
	private Long districtId; // required
	private Long financialYearId; // required (FY -> DAP -> District -> DemandCode -> Schemes)
	private Long schemeTypeId; // required (DemandCode ID, e.g., O27)
	private List<Long> schemeIds; // required (schemes to map under this DemandCode for this District)
	private String remarks; // optional
}
