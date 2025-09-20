// BudgetByFyDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BudgetByFyDto {
	private Long financialYearId;
	private String financialYear;
	private Double allocated;
	private Double utilized;
	private Double remaining;
}
