package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.service.MLCServiceImpl;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mlcs")
@RequiredArgsConstructor
public class MLCController {

	private final MLCServiceImpl mlcService;
	private final JwtUtil jwtUtil;

	// ✅ Fetch MLCs (nodal + non-nodal) for logged-in user district
	@GetMapping("/by-logged-in-user")
	public ResponseEntity<List<IdNameDto>> getMLCsForLoggedInUser(@RequestHeader("Authorization") String authHeader) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlcService.getMLCsForLoggedInUser(username));
	}

	// ✅ Fetch all MLCs (ID + Name only) with optional districtId filter
	@GetMapping("/all")
	public ResponseEntity<List<IdNameDto>> getAllMLCs(@RequestParam(value = "districtId", required = false) Long districtId) {
		return ResponseEntity.ok(mlcService.getAllMLCs(districtId));
	}
}