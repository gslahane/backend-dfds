// src/main/java/com/lsit/dfds/controller/MLCDashboardController.java
package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.MlcDashboardResponseDto;
import com.lsit.dfds.service.MLCDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mlc/dashboard")
@RequiredArgsConstructor
public class MLCDashboardController {

	private final MLCDashboardService service;
	private final JwtUtil jwtUtil;

	/**
	 * Visibility rules: - MLC: always own data (mlcId ignored). - STATE_*: all MLCs
	 * if mlcId is omitted; or specific MLC if mlcId provided. - DISTRICT_* /
	 * IA_ADMIN: must provide mlcId; if districtId omitted, auto-binds to caller's
	 * district. Optional: nodal filter (true = nodal only, false = non-nodal only).
	 */
	@GetMapping
	public ResponseEntity<MlcDashboardResponseDto> getDashboard(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long mlcId, @RequestParam(required = false) String search,
			@RequestParam(required = false) Boolean nodal,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "10") Integer size) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity
				.ok(service.getDashboard(username, financialYearId, districtId, mlcId, search, nodal, page, size));
	}
}