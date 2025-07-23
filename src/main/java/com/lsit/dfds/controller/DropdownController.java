package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.Division;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.service.DropdownService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dropdown")
@RequiredArgsConstructor
public class DropdownController {

	private final DropdownService dropdownService;
	private final JwtUtil jwtUtil;

	@GetMapping("/schemes")
	public ResponseEntity<?> getSchemesForAuthenticatedUser(HttpServletRequest request) {
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
			List<String> schemes = dropdownService.getSchemeNamesForUser(username);
			return ResponseEntity.ok(schemes);
		} catch (RuntimeException ex) {
			return ResponseEntity.status(404).body(ex.getMessage());
		}
	}

	@GetMapping("/divisions")
	public ResponseEntity<List<Division>> getAllDivisions() {
		return ResponseEntity.ok(dropdownService.getAllDivisions());
	}

	@GetMapping("/districts")
	public ResponseEntity<List<District>> getAllDistricts() {
		return ResponseEntity.ok(dropdownService.getAllDistricts());
	}

	@GetMapping("/districts/by-division/{divisionId}")
	public ResponseEntity<List<District>> getDistrictsByDivision(@PathVariable Long divisionId) {
		return ResponseEntity.ok(dropdownService.getDistrictsByDivisionId(divisionId));
	}

	@GetMapping("/talukas/by-district/{districtId}")
	public ResponseEntity<List<Taluka>> getTalukasByDistrict(@PathVariable Long districtId) {
		return ResponseEntity.ok(dropdownService.getTalukasByDistrictId(districtId));
	}

	@GetMapping("/constituencies/by-district/{districtId}")
	public ResponseEntity<List<Constituency>> getConstituenciesByDistrict(@PathVariable Long districtId) {
		return ResponseEntity.ok(dropdownService.getConstituenciesByDistrictId(districtId));
	}
}
