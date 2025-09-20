// src/main/java/com/lsit/dfds/controller/VendorDashboardController.java
package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.VendorDashboardResponseDto;
import com.lsit.dfds.service.VendorDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vendor/dashboard")
@RequiredArgsConstructor
public class VendorDashboardController {

	private final VendorDashboardService service;
	private final JwtUtil jwtUtil;

	/**
	 * Visibility: - VENDOR: sees own dashboard (vendorId param ignored). -
	 * IA/DISTRICT/STATE: must provide vendorId.
	 */
	@GetMapping
	public ResponseEntity<VendorDashboardResponseDto> getDashboard(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long vendorId, @RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long schemeId,
			@RequestParam(required = false) String status, // DemandStatuses enum name
			@RequestParam(required = false) String search) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity
				.ok(service.getDashboard(username, vendorId, financialYearId, districtId, schemeId, status, search));
	}
}
