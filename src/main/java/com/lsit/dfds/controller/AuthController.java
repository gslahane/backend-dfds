package com.lsit.dfds.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.service.AuditService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditService audit;

	@Autowired
	private MLARepository mlaRepository;

	@Autowired
	private MLCRepository mlcRepository;


	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			User user = userRepository.findByUsername(loginRequest.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));

			String token = jwtUtil.generateToken(user.getUsername());

			// district (null-safe)
			Long districtId = (user.getDistrict() != null) ? user.getDistrict().getId() : null;
			String districtName = (user.getDistrict() != null) ? user.getDistrict().getDistrictName() : null;

			// attach mlaId / mlcId if applicable
			Long mlaId = null;
			Long mlcId = null;

			if (user.getRole() == Roles.MLA) {
				MLA mla = mlaRepository.findByUser_Id(user.getId()).orElse(null);
				mlaId = (mla != null) ? mla.getId() : null;
			} else if (user.getRole() == Roles.MLC) {
				MLC mlc = mlcRepository.findByUser_Id(user.getId()).orElse(null);
				mlcId = (mlc != null) ? mlc.getId() : null;
			}

			LoginResponse resp = new LoginResponse(token, user.getId(), // userId
					user.getUsername(), // username
					user.getMobile(), // mobile
					(user.getRole() != null ? user.getRole().name() : null), // role
					user.getDesignation(), // designation
					user.getFullname(), // fullName
					districtId, // districtId
					districtName, // districtName
					mlaId, // mlaId (only if role == MLA)
					mlcId // mlcId (only if role == MLC)
			);

			audit.log(user.getUsername(), "LOGIN_SUCCESS", "User", user.getId(), null);
			return ResponseEntity.ok(resp);

		} catch (BadCredentialsException ex) {
			audit.log(loginRequest.getUsername(), "LOGIN_FAILED", "User", null, "Bad credentials");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		} catch (Exception ex) {
			audit.log(loginRequest.getUsername(), "LOGIN_FAILED", "User", null, ex.getMessage());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
		}
	}

	@GetMapping("/me")
	public ResponseEntity<?> getCurrentUser() {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		Optional<User> user = userRepository.findByUsername(username);
		return ResponseEntity.ok(user);
	}

	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest request) {
	    String authHeader = request.getHeader("Authorization");
	    if (authHeader != null && authHeader.startsWith("Bearer ")) {
	        String token = authHeader.substring(7); // store in blacklist
	        jwtUtil.blacklistToken(token);
	    }
	    SecurityContextHolder.clearContext();
	    return ResponseEntity.ok("Logged out successfully. Token expired/blacklisted.");
	}
}

// --------- DTOs (package-private in the same file is fine) ---------

class LoginRequest {
	private String username;
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}

class LoginResponse {
	private final String token;

	// NEW
	private final Long userId; // always returned
	private final String username; // convenient for clients

	private final Long mobile;
	private final String role;
	private final String designation;
	private final String fullName;
	private final Long districtId;
	private final String districtName;

	// NEW: only non-null for respective roles
	private final Long mlaId; // set if role == MLA
	private final Long mlcId; // set if role == MLC

	@Deprecated
	public LoginResponse(String token, Long mobile, String role, String designation, String fullName, Long districtId,
			String districtName) {
		this.token = token;
		this.userId = null;
		this.username = null;
		this.mobile = mobile;
		this.role = role;
		this.designation = designation;
		this.fullName = fullName;
		this.districtId = districtId;
		this.districtName = districtName;
		this.mlaId = null;
		this.mlcId = null;
	}

	public LoginResponse(String token, Long userId, String username, Long mobile, String role, String designation,
			String fullName, Long districtId, String districtName, Long mlaId, Long mlcId) {
		this.token = token;
		this.userId = userId;
		this.username = username;
		this.mobile = mobile;
		this.role = role;
		this.designation = designation;
		this.fullName = fullName;
		this.districtId = districtId;
		this.districtName = districtName;
		this.mlaId = mlaId;
		this.mlcId = mlcId;
	}

	public static LoginResponse ofBasic(String token, Long mobile, String role, String designation, String fullName,
			Long districtId, String districtName) {
		return new LoginResponse(token, null, null, mobile, role, designation, fullName, districtId, districtName, null,
				null);
	}

	public String getToken() {
		return token;
	}

	public Long getUserId() {
		return userId;
	}

	public String getUsername() {
		return username;
	}

	public Long getMobile() {
		return mobile;
	}

	public String getRole() {
		return role;
	}

	public String getDesignation() {
		return designation;
	}

	public String getFullName() {
		return fullName;
	}

	public Long getDistrictId() {
		return districtId;
	}

	public String getDistrictName() {
		return districtName;
	}

	public Long getMlaId() {
		return mlaId;
	}

	public Long getMlcId() {
		return mlcId;
	}
}