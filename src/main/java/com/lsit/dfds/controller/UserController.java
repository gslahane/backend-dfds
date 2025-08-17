package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.service.UserService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor // ✅ Auto-injects final fields
public class UserController {

	private final UserService userRegistrationService;
	private final JwtUtil jwtUtil;

	/**
	 * ✅ API to register a new user STATE_ADMIN → Can create DISTRICT users
	 * DISTRICT_ADMIN → Can create IA_ADMIN users
	 */
	@PostMapping("/register")
	public ResponseEntity<?> registerUser(@RequestHeader("Authorization") String authHeader,
			@RequestBody UserRegistrationDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(userRegistrationService.registerUser(dto, username));
	}
}
