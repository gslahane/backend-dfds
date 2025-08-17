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

			Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
			if (userOpt.isEmpty()) {
				audit.log(loginRequest.getUsername(), "LOGIN_FAILED", "User", null, "User not found");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password");
			}
			User user = userOpt.get();

			String token = jwtUtil.generateToken(user.getUsername());

			// keep existing response fields + add fullName
			LoginResponse resp = new LoginResponse(token, user.getMobile(), user.getAgencyCode(),
					user.getRole() != null ? user.getRole().name() : null, user.getDesignation(), user.getFullname() // NEW
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
	private String token;
	private Long mobile;
	private String agencyCode;
	private String role;
	private String designation;
	private String fullName; // NEW

	public LoginResponse(String token, Long mobile, String agencyCode, String role, String designation,
			String fullName) {
		this.token = token;
		this.mobile = mobile;
		this.agencyCode = agencyCode;
		this.role = role;
		this.designation = designation;
		this.fullName = fullName;
	}

	public String getToken() {
		return token;
	}

	public Long getMobile() {
		return mobile;
	}

	public String getAgencyCode() {
		return agencyCode;
	}

	public String getRole() {
		return role;
	}

	public String getDesignation() {
		return designation;
	}

	public String getFullName() {
		return fullName;
	} // NEW
}
