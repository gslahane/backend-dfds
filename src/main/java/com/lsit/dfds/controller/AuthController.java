package com.lsit.dfds.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		SecurityContextHolder.getContext().setAuthentication(authentication);

		Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
		if (userOpt.isEmpty()) {
			return ResponseEntity.badRequest().body("Invalid username or password");
		}
		User user = userOpt.get();

		String token = jwtUtil.generateToken(user.getUsername());

		return ResponseEntity.ok(new LoginResponse(token, user.getMobile(), user.getAgencyCode(), user.getRole().name(),
				user.getDesignation()));
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

	public LoginResponse(String token, Long mobile, String agencyCode, String role, String designation) {
		this.token = token;
		this.mobile = mobile;
		this.agencyCode = agencyCode;
		this.role = role;
		this.designation = designation;
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
}
