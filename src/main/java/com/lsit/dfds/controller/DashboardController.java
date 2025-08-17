package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.AdminDAPBudgetRespDto;
import com.lsit.dfds.dto.AdminDAPDashboardRequest;
import com.lsit.dfds.dto.AdminDAPDashboardResponce;
import com.lsit.dfds.dto.AdminDAPSchemesDto;
import com.lsit.dfds.dto.AdminMLDashboardDto;
import com.lsit.dfds.dto.AdminMLrequestDto;
import com.lsit.dfds.dto.OverallBudgetDto;
import com.lsit.dfds.service.IDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

	private final IDashboardService dashboardService;
	private final JwtUtil jwtUtil;

	@GetMapping("/getAdminDAPDashboard")
	public ResponseEntity<?> getAdminDAPDashboard(
			@RequestHeader(name = "Authorization", required = false) String authHeader, AdminDAPDashboardRequest request // assuming
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
			List<AdminDAPDashboardResponce> adminDAPDashboardResponce = dashboardService.getAdminDAPDashboard(username,
					request);

			return ResponseEntity.ok(adminDAPDashboardResponce);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getAdminMLDashboard")
	public ResponseEntity<?> getAdminMLDashboard(
			@RequestHeader(name = "Authorization", required = false) String authHeader, AdminMLrequestDto request // assuming
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
			List<AdminMLDashboardDto> adminDAPDashboardResponce = dashboardService.getAdminMLDashboard(username,
					request);

			return ResponseEntity.ok(adminDAPDashboardResponce);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getOverallStateBudget")
	public ResponseEntity<?> getOverallStateBudget(@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

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
			List<OverallBudgetDto> budget = dashboardService.getOverallStateBudget(username, financialYear_Id);
			return ResponseEntity.ok(budget);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getAdminDAPBudget")
	public ResponseEntity<?> getAdminDAPBudget(@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

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
			AdminDAPBudgetRespDto adminDAPBudgetRespDto = dashboardService.getAdminDAPBudget(username,
					financialYear_Id);
			return ResponseEntity.ok(adminDAPBudgetRespDto);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getAdminDAPSchemes")
	public ResponseEntity<?> getAdminDAPSchemes(@RequestParam Long district_Id, @RequestParam Long schemeType_Id,
			@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

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
			List<AdminDAPSchemesDto> adminDAPSchemesDto = dashboardService.getAdminDAPSchemes(username, district_Id,
					schemeType_Id, financialYear_Id);
			return ResponseEntity.ok(adminDAPSchemesDto);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getAdminMLBudget")
	public ResponseEntity<?> getAdminMLBudget(@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

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
			AdminDAPBudgetRespDto adminDAPBudgetRespDto = dashboardService.getAdminMLBudget(username, financialYear_Id);
			return ResponseEntity.ok(adminDAPBudgetRespDto);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/getAdminDistrictMLBudget")
	public ResponseEntity<?> getAdminDistrictMLBudget(@RequestParam Long financialYear_Id, HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");

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
			AdminDAPBudgetRespDto adminDAPBudgetRespDto = dashboardService.getAdminDistrictMLBudget(username,
					financialYear_Id);
			return ResponseEntity.ok(adminDAPBudgetRespDto);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

}
