package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.GenericResponseDto;
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

	// ✅ Secured: Activate / Deactivate User
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
	@GetMapping("/ia-users")
	public ResponseEntity<GenericResponseDto<List<UserWithBankDetailsDto>>> getIAUsersWithBankDetails(
			@RequestHeader("Authorization") String auth) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<UserWithBankDetailsDto> users = userService.getIAUsersWithBankDetails();

			if (users.isEmpty()) {
				return ResponseEntity.ok(new GenericResponseDto<>(true, "No IA users found", users));
			}

			return ResponseEntity.ok(new GenericResponseDto<>(true, "IA users fetched successfully", users));

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Failed to fetch IA users: Unauthorized", null));
		}
	}

}