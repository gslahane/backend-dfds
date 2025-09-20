package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.DistrictSchemeMappingDto;
import com.lsit.dfds.dto.SchemeCreateDto;
import com.lsit.dfds.dto.SchemeRespDto;
import com.lsit.dfds.dto.SchemeResponseDto;
import com.lsit.dfds.dto.SchemerequestDto;
import com.lsit.dfds.service.DistrictSchemeService;
import com.lsit.dfds.service.ISchemeService;
import com.lsit.dfds.service.SchemeCreateService;
import com.lsit.dfds.service.SchemeFetchService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/scheme-master")
@RequiredArgsConstructor
public class SchemeController {
	private final JwtUtil jwtUtil;
	private final ISchemeService schemeService;

	private final SchemeCreateService schemeCreateService;
	private final DistrictSchemeService districtSchemeService;
	private final SchemeFetchService schemeFetchService;

	@PostMapping("/create")
	public ResponseEntity<?> createScheme(@RequestBody SchemeCreateDto dto,
			@RequestHeader("Authorization") String authHeader) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		SchemeResponseDto response = schemeCreateService.createScheme(dto, username);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/mapWithDistrict")
	public ResponseEntity<?> mapSchemesToDistrict(@RequestHeader("Authorization") String authHeader,
			@RequestBody DistrictSchemeMappingDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(districtSchemeService.mapSchemesToDistrict(dto, username));
	}

	@GetMapping("/getAdminMLDashboard")
	public ResponseEntity<?> getAdminMLDashboard(
			@RequestHeader(name = "Authorization", required = false) String authHeader, SchemerequestDto request // assuming
																													// this
																													// comes
																													// from
																													// query
																													// params
	) {
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
			// Replace with the actual method name in your service
			List<SchemeRespDto> adminDAPDashboardResponce = schemeService.getAllScheme(username, request);

			return ResponseEntity.ok(adminDAPDashboardResponce);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}
}
