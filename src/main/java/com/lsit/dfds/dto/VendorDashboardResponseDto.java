// src/main/java/com/lsit/dfds/dto/VendorDashboardResponseDto.java
package com.lsit.dfds.dto;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class VendorDashboardResponseDto {

	// Top counters and totals
	private Long totalDemands;
	private Long pendingDemands;
	private Long rejectedDemands;
	private Long approvedByStateDemands;
	private Long transferredDemands;

	private Double totalGrossDemanded;
	private Double totalNetDemanded;
	private Double totalTransferred; // received from State

	// Optional work aggregates
	private Long totalWorks;
	private Map<String, Long> worksByStatus; // status -> count

	// Tables
	private List<VendorDemandRowDto> demands;
	private List<VendorWorkRowDto> works;
}
