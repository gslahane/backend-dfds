package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.MLABudgetPostDto;
import com.lsit.dfds.service.IMLABudgetService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mla-budget")
@RequiredArgsConstructor
public class MLABudgetController {

	private final IMLABudgetService mlaBudgetService;
	private final JwtUtil jwtUtil;

	@PostMapping("/save")
	public ResponseEntity<?> saveMLABudget(@RequestHeader("Authorization") String authHeader,
			@RequestBody MLABudgetPostDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlaBudgetService.saveMLABudget(dto, username));
	}

	@GetMapping("/fetch")
	public ResponseEntity<?> getMLABudgets(@RequestHeader("Authorization") String authHeader,
			@RequestParam Long financialYearId) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlaBudgetService.getMLABudgetSummary(username, financialYearId));
	}

	// Fetch all MLA budget details with MLA details
	@GetMapping("/fetch-all")
	public ResponseEntity<?> getAllMLABudgets(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long financialYearId) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlaBudgetService.getAllMLABudgetDetails(username, financialYearId));
	}

}
