package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.DAPBudgetResponceDto;
import com.lsit.dfds.dto.DAPDistrictBudgetPostDto;
import com.lsit.dfds.entity.DistrictLimitAllocation;
import com.lsit.dfds.service.IDistrictBudgetService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/DistrictBudget")
@RequiredArgsConstructor
public class DistrictBudgetController {
	private final IDistrictBudgetService districtBudgetService;
	private final JwtUtil jwtUtil;

	@GetMapping("/getDAPDistrictBudgets")
	public ResponseEntity<?> getDistrictDAPBudget(@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Authorization header missing or invalid");
		}

		String token = authHeader.substring(7);
		String username;

		try {
			username = jwtUtil.extractUsername(token);
		} catch (Exception e) {
			return ResponseEntity.status(401).body("Invalid token");
		}

		try {
			List<DAPBudgetResponceDto> dAPBudgetResponceDto = districtBudgetService.getDAPBudget(username,
					financialYear_Id);
			return ResponseEntity.ok(dAPBudgetResponceDto);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@PostMapping("/saveDistrictDAPBudget")
	public ResponseEntity<?> saveDistrictDAPBudget(@RequestBody DAPDistrictBudgetPostDto dto,
			HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(401).body("Authorization header missing or invalid");
		}

		String token = authHeader.substring(7);
		String username;

		try {
			username = jwtUtil.extractUsername(token);
		} catch (Exception e) {
			return ResponseEntity.status(401).body("Invalid token");
		}

		try {
			if (dto.getAllocatedLimit() == null || dto.getAllocatedLimit() < 0) {
				return ResponseEntity.badRequest().body("Allocated limit must be a non-negative number.");
			}
			DistrictLimitAllocation saved = districtBudgetService.saveDAPDistrictBudget(dto, username);
			return ResponseEntity.ok(saved);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(400).body("Error: " + ex.getMessage());
		}
	}

}
