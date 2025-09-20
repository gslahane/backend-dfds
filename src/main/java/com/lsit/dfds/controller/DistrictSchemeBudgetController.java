// src/main/java/com/lsit/dfds/controller/DistrictSchemeBudgetController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.DistrictSchemeBudgetDto;
import com.lsit.dfds.dto.SchemeBudgetResponseDto;
import com.lsit.dfds.service.DistrictSchemeBudgetService;
import com.lsit.dfds.service.SchemeFetchService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/district-scheme-budget")
@RequiredArgsConstructor
public class DistrictSchemeBudgetController {

	private final DistrictSchemeBudgetService budgetService;
	private final JwtUtil jwtUtil;
	private final SchemeFetchService schemeFetchService;

	private String extractUsername(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new RuntimeException("Missing/invalid Authorization header");
		}
		String token = authHeader.substring(7);
		return jwtUtil.extractUsername(token);
	}

	@PostMapping("/allocate")
	public ResponseEntity<?> allocateBudget(@RequestHeader("Authorization") String authHeader,
			@RequestBody DistrictSchemeBudgetDto dto) {
		try {
			String username = extractUsername(authHeader);
			return ResponseEntity.ok(budgetService.allocateBudgetToScheme(dto, username));
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
		}
	}

	@GetMapping("/fetch")
	public ResponseEntity<?> fetchSchemes(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long schemeTypeId, @RequestParam(required = false) String planType,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long financialYearId) {
		try {
			String username = extractUsername(authHeader);
			List<SchemeBudgetResponseDto> response = schemeFetchService.fetchSchemesWithFiltersForUser(username,
					schemeTypeId, planType, districtId, financialYearId);
			return ResponseEntity.ok(response);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
		}
	}
}
