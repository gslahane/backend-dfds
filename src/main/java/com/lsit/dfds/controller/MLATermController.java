package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.MlaTermUpsertDto;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.service.MlaTermService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mla/term")
@RequiredArgsConstructor
public class MLATermController {

	private final MlaTermService mlaTermService;
	private final JwtUtil jwtUtil;

	@PostMapping
	public ResponseEntity<MLATerm> upsertTerm(@RequestHeader("Authorization") String authHeader,
			@RequestBody MlaTermUpsertDto dto) {
		jwtUtil.extractUsername(authHeader.substring(7)); // authorize caller (e.g., STATE/DISTRICT admin)
		return ResponseEntity.ok(mlaTermService.upsertTerm(dto));
	}

	@GetMapping("/active/{mlaId}")
	public ResponseEntity<MLATerm> getActive(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long mlaId) {
		jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(mlaTermService.getActiveTerm(mlaId));
	}
}
