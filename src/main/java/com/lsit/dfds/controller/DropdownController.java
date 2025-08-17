package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.SchemeTypeDropdownDto;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;
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

	private final SchemesRepository schemesRepository;

//	@Autowired
//	private JwtUtil jwtUtil;

	@Autowired
	private UserRepository userRepository;

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

	private boolean isValidUser(String authHeader) {
		try {
			if (authHeader == null || !authHeader.startsWith("Bearer "))
				return false;
			String token = authHeader.substring(7);
			String username = jwtUtil.extractUsername(token);
			return userRepository.findByUsername(username).isPresent();
		} catch (Exception e) {
			return false;
		}
	}

	@GetMapping("/works/by-scheme-year-district")
	public ResponseEntity<?> getWorksBySchemeYearAndDistrict(@RequestParam Long schemeId,
			@RequestParam String financialYear, @RequestParam(required = false) Long districtId,
			@RequestHeader("Authorization") String authHeader) {

		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header missing or invalid");
		}

		String token = authHeader.substring(7);
		String username;
		try {
			username = jwtUtil.extractUsername(token);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
		}

		try {
			List<Work> works = dropdownService.getWorksBySchemeYearAndDistrict(username, schemeId, financialYear,
					districtId);
			return ResponseEntity.ok(works);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	@GetMapping("/financial-years")
	public ResponseEntity<?> getAllFinancialYears(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

		List<IdNameDto> years = dropdownService.getAllFinancialYears();
		return ResponseEntity.ok(years);
	}

	@GetMapping("/divisions")
	public ResponseEntity<?> getAllDivisions(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		return ResponseEntity.ok(dropdownService.getAllDivisions());
	}

	@GetMapping("/districts")
	public ResponseEntity<?> getAllDistricts(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		return ResponseEntity.ok(dropdownService.getAllDistricts());
	}

	@GetMapping("/districts/by-division/{divisionId}")
	public ResponseEntity<?> getDistrictsByDivision(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long divisionId) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		return ResponseEntity.ok(dropdownService.getDistrictsByDivisionId(divisionId));
	}

	@GetMapping("/talukas/by-district/{districtId}")
	public ResponseEntity<?> getTalukasByDistrict(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long districtId) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		return ResponseEntity.ok(dropdownService.getTalukasByDistrictId(districtId));
	}

	@GetMapping("/constituencies/by-district/{districtId}")
	public ResponseEntity<?> getConstituenciesByDistrict(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long districtId) {
		if (!isValidUser(authHeader))
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		return ResponseEntity.ok(dropdownService.getConstituenciesByDistrictId(districtId));
	}

	@GetMapping("/schemes/search")
	public ResponseEntity<?> searchSchemesForUser(@RequestParam(required = false) String districtName,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) String schemeType,
			HttpServletRequest request) {

		String username = jwtUtil.extractUsernameFromRequest(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
		}

		try {
			List<Schemes> result = dropdownService.searchSchemesByFilters(username, districtName, districtId,
					schemeType);
			return ResponseEntity.ok(result);
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		}
	}

	public String extractUsernameFromRequest(HttpServletRequest request) {
		final String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return jwtUtil.extractUsername(authHeader.substring(7)); // ✅ Fixed here
		}
		return null;
	}

	@GetMapping("/sectors")
	public ResponseEntity<List<IdNameDto>> getAllSectors() {
		return ResponseEntity.ok(dropdownService.getAllSectors());
	}

	@GetMapping("/schemes/by-sector/{sectorId}")
	public ResponseEntity<List<IdNameDto>> getSchemesBySector(@PathVariable Long sectorId) {
		return ResponseEntity.ok(dropdownService.getSchemesBySectorId(sectorId));
	}

	@GetMapping("/scheme-types")
	public ResponseEntity<List<SchemeTypeDropdownDto>> getAllSchemeTypes() {
		return ResponseEntity.ok(dropdownService.getAllSchemeTypesWithDetails());
	}

	@GetMapping("/schemes/dap")
	public ResponseEntity<List<IdNameDto>> getDapSchemesForPath(@RequestParam Long financialYearId,
			@RequestParam Long districtId, @RequestParam Long demandCodeId) {
		var list = schemesRepository.findDistrictSchemesForDemandCodeDAP(districtId, financialYearId, demandCodeId)
				.stream().map(s -> new IdNameDto(s.getId(), s.getSchemeName())).toList();
		return ResponseEntity.ok(list);
	}

}
