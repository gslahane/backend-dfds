// src/main/java/com/lsit/dfds/dto/DistrictOverviewDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistrictOverviewDto {
	private Double totalBudget; // sum of allocated budget rows in scope
	private Double disbursed; // sum(netPayable) of TRANSFERRED demands in scope
	private Double balance; // totalBudget - disbursed (or from budget remaining if you prefer)
	private Long pendingDemands; // count of pending-at-district or pending-at-state (see service)
}
