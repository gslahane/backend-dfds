package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.MLCBudgetPostDto;
import com.lsit.dfds.service.MLCBudgetService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mlc-budget")
@RequiredArgsConstructor
public class MLCBudgetController {

	private final JwtUtil jwtUtil;
	private final MLCBudgetService mlcBudgetService;

	@GetMapping("/getMLCBudgets")
	public ResponseEntity<?> getMLCBudgets(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long financialYearId) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlcBudgetService.getMLCBudgetSummary(username, financialYearId));
	}

	@PostMapping("/saveMLCBudget")
	public ResponseEntity<?> saveMLCBudget(@RequestHeader("Authorization") String authHeader,
			@RequestBody MLCBudgetPostDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlcBudgetService.saveMLCBudget(dto, username));
	}
}
