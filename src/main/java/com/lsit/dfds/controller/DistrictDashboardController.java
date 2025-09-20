// src/main/java/com/lsit/dfds/controller/DistrictDashboardController.java
package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.DistrictDashboardFilterDto;
import com.lsit.dfds.dto.DistrictDashboardResponseDto;
import com.lsit.dfds.service.DistrictDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/district/dashboard")
@RequiredArgsConstructor
public class DistrictDashboardController {

	private final DistrictDashboardService service;
	private final JwtUtil jwtUtil;

	@GetMapping
	public ResponseEntity<DistrictDashboardResponseDto> getDashboard(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) String planType, // "MLA","MLC","HADP","DAP"
																													// or
																													// null
			@RequestParam(required = false) Long mlaId, @RequestParam(required = false) Long mlcId,
			@RequestParam(required = false) String search) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		var filter = new DistrictDashboardFilterDto();
		filter.setFinancialYearId(financialYearId);
		filter.setPlanType(planType != null ? com.lsit.dfds.enums.PlanType.valueOf(planType) : null);
		filter.setMlaId(mlaId);
		filter.setMlcId(mlcId);
		filter.setSearch(search);

		return ResponseEntity.ok(service.getDashboard(username, filter));
	}
}
