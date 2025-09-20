package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.service.MLAServiceImpl;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mlas")
@RequiredArgsConstructor
public class MLAController {

	private final MLAServiceImpl mlaService;
	private final JwtUtil jwtUtil;

	@GetMapping("/names")
	public ResponseEntity<List<IdNameDto>> getMLANames(@RequestParam(defaultValue = "true") boolean onlyActive) {
		return ResponseEntity.ok(mlaService.getAllMLANames(onlyActive));
	}

	// ✅ New API to fetch MLAs by Constituency ID
	@GetMapping("/by-constituency/{constituencyId}")
	public ResponseEntity<List<IdNameDto>> getMLAsByConstituency(@PathVariable Long constituencyId) {
		return ResponseEntity.ok(mlaService.getMLAsByConstituency(constituencyId));
	}

	// ✅ API to fetch MLAs based on logged-in user's district
	@GetMapping("/by-logged-in-user")
	public ResponseEntity<List<IdNameDto>> getMLAsForLoggedInUser(@RequestHeader("Authorization") String authHeader) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));

		return ResponseEntity.ok(mlaService.getMLAsForLoggedInUser(username));
	}

}
