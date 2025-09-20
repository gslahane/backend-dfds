package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserListResponseDto;
import com.lsit.dfds.dto.UserStatusUpdateDto;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.service.UserManagementService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

/**
 * Comprehensive User Management Controller Handles user activation/deactivation
 * with proper authorization hierarchy
 */
@RestController
@RequestMapping("/api/user-management")
@RequiredArgsConstructor
public class UserManagementController {

	private final UserManagementService userManagementService;
	private final JwtUtil jwt;
	private final ObjectMapper objectMapper;

	// ==================== USER STATUS MANAGEMENT ====================

	/**
	 * Update user status (activate/deactivate/enable/disable) Only STATE users can
	 * manage all users, DISTRICT users can only manage IA_ADMIN users in their
	 * district
	 */
	@PutMapping(value = "/users/{userId}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<UserDto>> updateUserStatus(@RequestHeader("Authorization") String auth,
			@PathVariable Long userId, @RequestPart("payload") String payload,
			@RequestPart(name = "letter", required = false) MultipartFile letter) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			UserStatusUpdateDto dto = objectMapper.readValue(json, UserStatusUpdateDto.class);

			GenericResponseDto<UserDto> response = userManagementService.updateUserStatus(username, userId, dto,
					letter);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to update user status: " + e.getMessage(), null));
		}
	}

	// ==================== USER FETCHING APIs ====================

	/**
	 * Get all users based on actor's role and permissions
	 */
	@GetMapping("/users")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getAllUsers(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) Roles role,
			@RequestParam(required = false) Statuses status, @RequestParam(required = false) Long districtId) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getAllUsers(username, role,
					status, districtId);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch users: " + e.getMessage(), null));
		}
	}

	/**
	 * Get users by specific role
	 */
	@GetMapping("/users/by-role/{role}")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getUsersByRole(
			@RequestHeader("Authorization") String auth, @PathVariable Roles role) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getUsersByRole(username,
					role);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch users by role: " + e.getMessage(), null));
		}
	}

	/**
	 * Get users by district
	 */
	@GetMapping("/users/by-district/{districtId}")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getUsersByDistrict(
			@RequestHeader("Authorization") String auth, @PathVariable Long districtId) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getUsersByDistrict(username,
					districtId);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(
					new GenericResponseDto<>(false, "Failed to fetch users by district: " + e.getMessage(), null));
		}
	}

	/**
	 * Get user details by ID
	 */
	@GetMapping("/users/{userId}")
	public ResponseEntity<GenericResponseDto<UserDto>> getUserById(@RequestHeader("Authorization") String auth,
			@PathVariable Long userId) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<UserDto> response = userManagementService.getUserById(username, userId);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch user details: " + e.getMessage(), null));
		}
	}

	// ==================== SPECIFIC ROLE APIs ====================

	/**
	 * Get all MLA users with their status and district information
	 */
	@GetMapping("/mla-users")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getAllMlaUsers(
			@RequestHeader("Authorization") String auth) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getAllMlaUsers(username);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch MLA users: " + e.getMessage(), null));
		}
	}

	/**
	 * Get all MLC users with their status and district information
	 */
	@GetMapping("/mlc-users")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getAllMlcUsers(
			@RequestHeader("Authorization") String auth) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getAllMlcUsers(username);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch MLC users: " + e.getMessage(), null));
		}
	}

	/**
	 * Get all IA users with their status and district information
	 */
	@GetMapping("/ia-users")
	public ResponseEntity<GenericResponseDto<List<UserListResponseDto>>> getAllIaUsers(
			@RequestHeader("Authorization") String auth) {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			GenericResponseDto<List<UserListResponseDto>> response = userManagementService.getAllIaUsers(username);

			if (response.isSuccess()) {
				return ResponseEntity.ok(response);
			} else {
				return ResponseEntity.badRequest().body(response);
			}
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch IA users: " + e.getMessage(), null));
		}
	}

	// ==================== HELPER METHODS ====================

	/**
	 * Clean multipart JSON string by removing quotes and escaping
	 */
	private String cleanMultipartJson(String json) {
		if (json == null) {
			return null;
		}
		return json.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
	}
}
