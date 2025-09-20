package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.SpillCreateDto;
import com.lsit.dfds.dto.SpillDecisionDto;
import com.lsit.dfds.service.WorkSpillService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/spill-works")
@RequiredArgsConstructor
public class WorkSpillController {
	private final WorkSpillService service;
	private final JwtUtil jwt;

	@PostMapping("/request")
	public ResponseEntity<?> create(@RequestHeader("Authorization") String auth, @RequestBody SpillCreateDto dto) {
		String user = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.createSpill(user, dto));
	}

	@PutMapping("/decide")
	public ResponseEntity<?> decide(@RequestHeader("Authorization") String auth, @RequestBody SpillDecisionDto dto) {
		String user = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.decide(user, dto));
	}

	// Example list endpoint — implement repository filters similarly to MLA/MLC
	// dashboards
	@GetMapping
	public ResponseEntity<?> list(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long demandCodeId, @RequestParam(required = false) String search) {
		// Resolve by role like you did elsewhere (STATE sees all, DISTRICT
		// auto-filters, IA auto-filters)
		// Return a trimmed DTO for grid display.
		return ResponseEntity.ok("Implement finder using WorkSpillRequestRepository with role-based filters");
	}
}
