// src/main/java/com/lsit/dfds/dto/SchemeWithBudgetDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SchemeWithBudgetDto {
	private Long schemeId;
	private String schemeName;
	private Double allocated;
	private Double utilized;
	private Double remaining;
}
