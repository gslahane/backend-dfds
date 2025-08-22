package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.service.UserService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor // ✅ Auto-injects final fields
public class UserController {

	private final UserService userService;
	private final JwtUtil jwt;

	// STATE: can register district leadership/staff
	@PostMapping("/district-staff")
	public ResponseEntity<?> registerDistrictStaff(@RequestHeader("Authorization") String auth,
			@RequestBody UserRegistrationDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		// allow roles in this bucket only
		// DISTRICT_COLLECTOR, DISTRICT_DPO, DISTRICT_ADPO, DISTRICT_ADMIN,
		// DISTRICT_CHECKER, DISTRICT_MAKER
		return ResponseEntity.ok(userService.registerDistrictUser(dto, createdBy));
	}

	// DISTRICT_* (and STATE): register IA_ADMIN
	@PostMapping("/ia")
	public ResponseEntity<?> registerIaAdmin(@RequestHeader("Authorization") String auth,
			@RequestBody UserRegistrationDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		dto.setRole(Roles.IA_ADMIN);
		return ResponseEntity.ok(userService.registerUser(dto, createdBy));
	}

	// STATE: register MLA (new or link existing)
	@PostMapping("/mla")
	public ResponseEntity<?> registerMla(@RequestHeader("Authorization") String auth,
			@RequestBody UserRegistrationDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		dto.setRole(Roles.MLA);
		return ResponseEntity.ok(userService.registerUser(dto, createdBy));
	}

	// STATE: register MLC (new or link existing)
	@PostMapping("/mlc")
	public ResponseEntity<?> registerMlc(@RequestHeader("Authorization") String auth,
			@RequestBody UserRegistrationDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		dto.setRole(Roles.MLC);
		return ResponseEntity.ok(userService.registerUser(dto, createdBy));
	}

	// STATE or DISTRICT: register HADP admin (scoped by district/taluka)
	@PostMapping("/hadp-admin")
	public ResponseEntity<?> registerHadpAdmin(@RequestHeader("Authorization") String auth,
			@RequestBody UserRegistrationDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		dto.setRole(Roles.HADP_ADMIN);
		return ResponseEntity.ok(userService.registerUser(dto, createdBy));
	}
}