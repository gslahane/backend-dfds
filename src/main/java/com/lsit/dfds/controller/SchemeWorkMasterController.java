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

import com.lsit.dfds.dto.AssignWorkRequestDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.WorkDetailsDto;
import com.lsit.dfds.service.SchemeWorkMasterService;
import com.lsit.dfds.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scheme-work-master")
@RequiredArgsConstructor
public class SchemeWorkMasterController {

	private final SchemeWorkMasterService schemeWorkMasterService;
	private final JwtUtil jwtUtil;

	@GetMapping("/schemes")
	public ResponseEntity<List<IdNameDto>> getSchemesForDistrict(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) String planType, @RequestParam(required = false) Long districtId) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(schemeWorkMasterService.getSchemesForDistrict(username, planType, districtId));
	}

	@GetMapping("/ia-users")
	public ResponseEntity<List<IdNameDto>> getIAUsersForDistrict(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long districtId) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(schemeWorkMasterService.getIAUsers(username, districtId));
	}

	@PostMapping("/assign-work")
	public ResponseEntity<?> assignWork(@RequestHeader("Authorization") String authHeader,
			@RequestBody AssignWorkRequestDto dto) { // ✅ Correct for JSON

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(schemeWorkMasterService.assignWork(dto, username));
	}

	@Operation(summary = "Get assigned works", description = "Fetch works assigned based on filters such as planType, schemeId, iaUserId, and search keyword", parameters = {
			@Parameter(name = "Authorization", description = "JWT token", required = true),
			@Parameter(name = "planType", description = "Plan type (e.g. STATE, CENTRAL)", required = false),
			@Parameter(name = "schemeId", description = "Scheme ID", required = false),
			@Parameter(name = "iaUserId", description = "Implementing agency user ID", required = false),
			@Parameter(name = "search", description = "Search keyword (e.g. work name/code)", required = false) }, responses = {
					@ApiResponse(responseCode = "200", description = "List of assigned works", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WorkDetailsDto.class))) })
	@GetMapping("/works")
	public ResponseEntity<?> getAssignedWorks(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) String planType, @RequestParam(required = false) Long schemeId,
			@RequestParam(required = false) Long iaUserId, @RequestParam(required = false) String search) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity
				.ok(schemeWorkMasterService.getAssignedWorks(username, planType, schemeId, iaUserId, search));
	}

}
