package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.service.IADashboardService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ia-dashboard")
@RequiredArgsConstructor
public class IADashboardController {

	private final IADashboardService iaDashboardService;
	private final JwtUtil jwtUtil;

	@GetMapping
	public ResponseEntity<?> getIADashboard(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) String financialYear, @RequestParam(required = false) String planType, // DAP,
																													// MLA,
																													// MLC,
																													// HADP
			@RequestParam(required = false) Long schemeId, @RequestParam(required = false) Long workId,
			@RequestParam(required = false) String status) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));

		return ResponseEntity
				.ok(iaDashboardService.getDashboardData(username, financialYear, planType, schemeId, workId, status));
	}
}
