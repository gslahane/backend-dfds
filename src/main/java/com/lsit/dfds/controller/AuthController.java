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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.entity.User;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.service.AuditService;
import com.lsit.dfds.utils.JwtUtil;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuditService audit;

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
		try {
			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
			SecurityContextHolder.getContext().setAuthentication(authentication);

			User user = userRepository.findByUsername(loginRequest.getUsername())
					.orElseThrow(() -> new UsernameNotFoundException("User not found"));

			String token = jwtUtil.generateToken(user.getUsername());

			// district (null-safe)
			Long districtId = (user.getDistrict() != null) ? user.getDistrict().getId() : null;
			String districtName = (user.getDistrict() != null) ? user.getDistrict().getDistrictName() : null;

			LoginResponse resp = new LoginResponse(token, user.getMobile(), // <-- correct field
					user.getRole() != null ? user.getRole().name() : null, user.getDesignation(), user.getFullname(),
					districtId, // <-- id
					districtName // <-- name
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
	public ResponseEntity<?> logout() {
		SecurityContextHolder.clearContext();
		return ResponseEntity.ok("Logged out successfully. Please clear token on client side.");
	}
}

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
	private final Long mobile;
//	private final String agencyCode;
	private final String role;
	private final String designation;
	private final String fullName;
	private final Long districtId; // NEW
	private final String districtName; // NEW

	public LoginResponse(String token, Long mobile, String role, String designation, String fullName, Long districtId,
			String districtName) {
		this.token = token;
		this.mobile = mobile;
		this.role = role;
		this.designation = designation;
		this.fullName = fullName;
		this.districtId = districtId;
		this.districtName = districtName;
	}

	public String getToken() {
		return token;
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
}
