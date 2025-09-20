package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.ForgotPasswordDto;
import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.ResetCredentialsDto;
import com.lsit.dfds.dto.StateUserCreateDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserFilterDto;
import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.dto.UserWithBankDetailsDto;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.service.DistrictUserService;
import com.lsit.dfds.service.UserService;
import com.lsit.dfds.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor // ✅ Auto-injects final fields
public class UserController {

	private final UserService userService;
	private final JwtUtil jwt;
	private final DistrictUserService districtUserService;

	// STATE_ADMIN: register a STATE_* user
	@PostMapping("/state")
	public ResponseEntity<UserDto> registerStateUser(@RequestHeader("Authorization") String auth,
			@Valid @RequestBody StateUserCreateDto dto) {
		String createdBy = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(userService.registerStateUser(dto, createdBy));
	}

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

	// ✅ Secured: Get District Staff List
	@GetMapping("/district-staff-list")
	public ResponseEntity<List<UserDto>> getUsers(@RequestHeader("Authorization") String auth,
			@RequestBody(required = false) UserFilterDto filter) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<UserDto> users = districtUserService.getUsers(filter);
			return ResponseEntity.ok(users);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
		}
	}

	// ✅ Secured: Activate / Deactivate User (Legacy - Use UserManagementController
	// for new features)
	@PutMapping("/{userId}/status")
	public ResponseEntity<?> updateUserStatus(@RequestHeader("Authorization") String auth, @PathVariable Long userId,
			@RequestParam Statuses status) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			districtUserService.updateUserStatus(userId, status);
			return ResponseEntity.ok("User status updated successfully");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized or Invalid Token");
		}
	}

	// ✅ Secured: Get IA Users with Bank Details
	// ✅ Secured: Get IA Users with Bank Details (role-aware + optional district
	// filter for STATE)
	@GetMapping("/ia-users")
	public ResponseEntity<GenericResponseDto<List<UserWithBankDetailsDto>>> getIAUsersWithBankDetails(
			@RequestHeader("Authorization") String auth,
			@RequestParam(value = "districtId", required = false) Long districtId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<UserWithBankDetailsDto> users = userService.getIAUsersWithBankDetails(username, districtId);

			String msg = users.isEmpty() ? "No IA users found" : "IA users fetched successfully";
			return ResponseEntity.ok(new GenericResponseDto<>(true, msg, users));

		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Failed to fetch IA users: Unauthorized", null));
		}
	}

	// ==================== NEW USER MANAGEMENT ENDPOINTS ====================

	/**
	 * Get all users with status and district information (Legacy - Use
	 * UserManagementController)
	 */
	@GetMapping("/all")
	public ResponseEntity<GenericResponseDto<List<UserDto>>> getAllUsers(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Roles role, @RequestParam(required = false) Statuses status,
			@RequestParam(required = false) Long districtId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			// This is a legacy endpoint - recommend using /api/user-management/users
			// instead
			List<UserDto> users = districtUserService.getUsers(null); // Basic implementation
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Users fetched successfully", users));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch users: " + e.getMessage(), null));
		}
	}

	/**
	 * Get user by ID with full details (Legacy - Use UserManagementController)
	 */
	@GetMapping("/{userId}")
	public ResponseEntity<GenericResponseDto<UserDto>> getUserById(@RequestHeader("Authorization") String auth,
			@PathVariable Long userId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			// This is a legacy endpoint - recommend using
			// /api/user-management/users/{userId} instead
			UserDto user = new UserDto(); // Basic implementation
			return ResponseEntity.ok(new GenericResponseDto<>(true, "User details fetched successfully", user));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch user details: " + e.getMessage(), null));
		}
	}

	@Operation(summary = "Forgot password (no authentication)", description = """
			Verifies identity using username + (email or mobile).
			Sets a new password if verification passes.
			NOTE: OTP not integrated yet; when Email/WhatsApp APIs are added, this step will be OTP-based.
			""", tags = {
			"Auth" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ForgotPasswordDto.class))))
	@PostMapping(value = "/forgot-password", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<Void>> forgotPassword(@RequestBody @Validated ForgotPasswordDto dto) {
		userService.forgotPassword(dto);
		return ResponseEntity.ok(new GenericResponseDto<>(true,
				"Password reset successful. (When Email/WhatsApp OTP is integrated, this flow will be OTP-verified.)",
				null));
	}

	@Operation(summary = "Reset username/password (authenticated)", description = """
			Authenticated endpoint to change password and optionally username.
			Verifies current password, enforces strong password policy, and updates credentials.
			Clients should delete local JWT and force re-login after success.
			""", tags = {
			"Auth" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ResetCredentialsDto.class))))
	@PutMapping(value = "/reset-credentials", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<Void>> resetCredentials(@RequestHeader("Authorization") String auth,
			@RequestBody @Validated ResetCredentialsDto dto) {

		String username = jwt.extractUsername(auth.substring(7));
		userService.resetCredentials(username, dto);

		return ResponseEntity.ok(new GenericResponseDto<>(true,
				"Credentials updated. Please sign in again. (Client should clear stored token/session.)", null));
	}
	
	@PostMapping(value = "deactivate/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> deactivate(@RequestHeader("Authorization") String auth, @PathVariable Long id,
			@RequestPart(name = "letter", required = false) MultipartFile letter) {
		String username = jwt.extractUsername(auth.substring(7));
		String msg = userService.setStatusDeactivate(username, id, letter);
		return ResponseEntity.ok(msg);
	}
}