// src/main/java/com/lsit/dfds/controller/SchemeReappropriationController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.ReappropCreateDto;
import com.lsit.dfds.dto.ReappropDecisionDto;
import com.lsit.dfds.dto.ReappropViewDto;
import com.lsit.dfds.dto.SchemeWithBudgetDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.ReappropriationStatuses;
import com.lsit.dfds.service.SchemeReappropriationService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reappropriations")
@RequiredArgsConstructor
public class SchemeReappropriationController {

	private final SchemeReappropriationService service;
	private final JwtUtil jwt;

	// ---- District options ----
	@GetMapping("/district/demand-codes")
	public ResponseEntity<List<DemandCode>> demandCodes(@RequestHeader("Authorization") String auth,
			@RequestParam Long financialYearId, @RequestParam(required = false) PlanType planType) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.listDemandCodesForDistrict(username, financialYearId, planType));
	}

	@GetMapping("/district/schemes")
	public ResponseEntity<List<SchemeWithBudgetDto>> schemesWithBudgets(@RequestHeader("Authorization") String auth,
			@RequestParam Long financialYearId, @RequestParam Long demandCodeId) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.listSchemesWithBudgetsForDistrict(username, financialYearId, demandCodeId));
	}

	// ---- District create request ----
	@PostMapping("/district/create")
	public ResponseEntity<ReappropViewDto> create(@RequestHeader("Authorization") String auth,
			@RequestBody ReappropCreateDto dto) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.createRequest(username, dto));
	}

	// ---- State search/list ----
	@GetMapping("/state")
	public ResponseEntity<List<ReappropViewDto>> listForState(@RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long demandCodeId,
			@RequestParam(required = false) ReappropriationStatuses status) {
		return ResponseEntity.ok(service.listForState(financialYearId, districtId, demandCodeId, status));
	}

	// ---- District list own ----
	@GetMapping("/district")
	public ResponseEntity<List<ReappropViewDto>> listForDistrict(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long demandCodeId,
			@RequestParam(required = false) ReappropriationStatuses status) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.listForDistrict(username, financialYearId, demandCodeId, status));
	}

	// ---- State decision ----
	@PostMapping("/state/decision")
	public ResponseEntity<ReappropViewDto> decide(@RequestHeader("Authorization") String auth,
			@RequestBody ReappropDecisionDto dto) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.decide(username, dto));
	}
}
