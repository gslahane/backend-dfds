// MlaDashboardFilterDto.java  (for STATE view)
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MlaDashboardFilterDto {
	private Long financialYearId; // optional
	private Long districtId; // optional
	private Long constituencyId; // optional
	private Long mlaId; // optional
	private String search; // optional free text on work name / demandId
}
