package com.lsit.dfds.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.HadpTalukaOptionDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.service.DropdownService;
import com.lsit.dfds.service.SchemeWorkMasterService;
import com.lsit.dfds.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dropdown")
@RequiredArgsConstructor
public class DropdownController {

	private final DropdownService dropdownService;
	private final JwtUtil jwtUtil;
	private final HADPRepository hadpRepo;

	private final SchemesRepository schemesRepository;
	private final SchemeWorkMasterService schemeWorkMasterService;

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
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return false;
			}
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
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}

		List<IdNameDto> years = dropdownService.getAllFinancialYears();
		return ResponseEntity.ok(years);
	}

	@GetMapping("/divisions")
	public ResponseEntity<?> getAllDivisions(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getAllDivisions());
	}

	@GetMapping("/districts")
	public ResponseEntity<?> getAllDistricts(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getAllDistricts());
	}

	@GetMapping("/districts/by-division/{divisionId}")
	public ResponseEntity<?> getDistrictsByDivision(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long divisionId) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getDistrictsByDivisionId(divisionId));
	}

	@GetMapping("/talukas/by-district/{districtId}")
	public ResponseEntity<?> getTalukasByDistrict(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long districtId) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getTalukasByDistrictId(districtId));
	}

	@GetMapping("/constituencies/by-district/{districtId}")
	public ResponseEntity<?> getConstituenciesByDistrict(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long districtId) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
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

	// ---- Auth-guarded (same style as /talukas/by-district) ----

	@GetMapping("/sectors")
	public ResponseEntity<?> getAllSectors(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getAllSectors());
	}

	@GetMapping("/schemes/by-sector/{sectorId}")
	public ResponseEntity<?> getSchemesBySector(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long sectorId) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getSchemesBySectorId(sectorId));
	}

	@GetMapping("/scheme-types")
	public ResponseEntity<?> getAllSchemeTypes(@RequestHeader("Authorization") String authHeader) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}
		return ResponseEntity.ok(dropdownService.getAllSchemeTypesWithDetails());
	}

	@GetMapping("/schemes/dap")
	public ResponseEntity<?> getDapSchemesForPath(@RequestHeader("Authorization") String authHeader,
			@RequestParam Long financialYearId, @RequestParam Long districtId, @RequestParam Long demandCodeId) {
		if (!isValidUser(authHeader)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
		}

		var list = schemesRepository.findDistrictSchemesForDemandCodeDAP(districtId, financialYearId, demandCodeId)
				.stream().map(s -> new IdNameDto(s.getId(), s.getSchemeName())).toList();
		return ResponseEntity.ok(list);
	}

	@Operation(summary = "List IA users (optionally filtered by district)", description = """
			- STATE: if `districtId` omitted → returns ALL IA users; if provided → filters to that district.
			- DISTRICT/IA: returns only IA users of caller's district. If `districtId` provided, it must match caller's district.
			""", tags = {
			"Users", "Dropdowns" })
	@GetMapping("/ia-users")
	public ResponseEntity<?> getIAUsersForDistrict(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long districtId) {

		try {
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authorization header missing or invalid");
			}
			String username = jwtUtil.extractUsername(authHeader.substring(7));
			List<IdNameDto> list = schemeWorkMasterService.getIAUsers(username, districtId);
			return ResponseEntity.ok(list);
		} catch (RuntimeException ex) {
			// Unauthorized / Forbidden / Not found messages come through here
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
		}
	}

	// 1) All HADP talukas (distinct), optionally filtered by district/FY/scheme
	@GetMapping("/hadp-talukas")
	public ResponseEntity<GenericResponseDto<List<HadpTalukaOptionDto>>> getHadpTalukas(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long schemeId) {
		String username = jwtUtil.extractUsername(auth.substring(7));
		User u = userRepository.findByUsername(username).orElse(null);

		// Default district to the caller’s district if not provided and available
		if (districtId == null && u != null && u.getDistrict() != null) {
			districtId = u.getDistrict().getId();
		}

		List<HADP> rows = hadpRepo.findActiveHadpRows(districtId, financialYearId, schemeId, null);

		// Distinct by taluka id, keep first type if multiple rows exist
		var options = rows.stream().filter(h -> h.getTaluka() != null)
				.collect(Collectors.toMap(h -> h.getTaluka().getId(),
						h -> new HadpTalukaOptionDto(h.getTaluka().getId(), h.getTaluka().getTalukaName(),
								h.getType() != null ? h.getType().name() : null),
						(a, b) -> a, // keep first
						LinkedHashMap::new))
				.values().stream().collect(Collectors.toList());

		return ResponseEntity.ok(new GenericResponseDto<>(true, "HADP talukas fetched", options));
	}

	// 2) HADP talukas filtered by type (GROUP / SUB_GROUP)
	@GetMapping("/hadp-talukas/by-type")
	public ResponseEntity<GenericResponseDto<List<HadpTalukaOptionDto>>> getHadpTalukasByType(
			@RequestHeader("Authorization") String auth, @RequestParam HadpType type,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) Long schemeId) {
		String username = jwtUtil.extractUsername(auth.substring(7));
		User u = userRepository.findByUsername(username).orElse(null);

		if (districtId == null && u != null && u.getDistrict() != null) {
			districtId = u.getDistrict().getId();
		}

		List<HADP> rows = hadpRepo.findActiveHadpRows(districtId, financialYearId, schemeId, type);

		var options = rows.stream().filter(h -> h.getTaluka() != null)
				.collect(Collectors.toMap(h -> h.getTaluka().getId(),
						h -> new HadpTalukaOptionDto(h.getTaluka().getId(), h.getTaluka().getTalukaName(),
								h.getType() != null ? h.getType().name() : null),
						(a, b) -> a, LinkedHashMap::new))
				.values().stream().collect(Collectors.toList());

		return ResponseEntity.ok(new GenericResponseDto<>(true, "HADP talukas by type fetched", options));
	}

}
