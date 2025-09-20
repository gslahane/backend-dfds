package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.HADPBudgetAllocationDto;
import com.lsit.dfds.dto.HADPBudgetResponseDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.service.HADPBudgetService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hadp-budget")
@RequiredArgsConstructor
public class HADPBudgetController {

	private final HADPBudgetService hadpBudgetService;
	private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('STATE_ADMIN')")
	@PostMapping("/allocate")
	public ResponseEntity<?> allocateBudget(@RequestHeader("Authorization") String authHeader,
			@RequestBody HADPBudgetAllocationDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(hadpBudgetService.allocateBudget(dto, username));
	}

	@GetMapping("/fetch")
	public ResponseEntity<?> fetchBudgets(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long hadpTalukaId,
			@RequestParam(required = false) Long financialYearId) {
		try {
			// Extract username from JWT token (after "Bearer " prefix)
			String username = jwtUtil.extractUsername(authHeader.substring(7));

			// Fetch budgets based on the extracted username and request parameters
			List<HADPBudgetResponseDto> budgets = hadpBudgetService.fetchBudgets(username, districtId, hadpTalukaId,
					financialYearId);

			// If no budgets found, return No Content (204)
			if (budgets.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No budgets found.");
			}

			// Return the budgets with a successful status (200)
			return ResponseEntity.ok(budgets);

		} catch (Exception e) {
			// Return an error response with the appropriate status code and error message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching budgets: " + e.getMessage());
		}
	}

	// Endpoint to fetch districts based on HADP Plan Type (GROUP or SUB_GROUP)
	@GetMapping("/districts-by-plan-type")
	public ResponseEntity<?> getDistrictsByPlanType(@RequestHeader("Authorization") String authHeader,
			@RequestParam HadpType planType) {
		try {
			// Extract the username from the token (after "Bearer " prefix)
			String username = jwtUtil.extractUsername(authHeader.substring(7));

			// Fetch districts based on the HADP Plan Type
			List<IdNameDto> districts = hadpBudgetService.getDistrictsByHadpPlanType(planType);

			// If no districts found, return NO_CONTENT
			if (districts.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body("No districts found for the specified plan type.");
			}

			// Return the districts with a successful response
			return ResponseEntity.ok(districts);
		} catch (Exception e) {
			// Return an error response with an appropriate message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching districts: " + e.getMessage());
		}
	}

	// Endpoint to fetch talukas based on district ID
	@GetMapping("/talukas-by-district")
	public ResponseEntity<?> getTalukasByDistrict(@RequestHeader("Authorization") String authHeader,
			@RequestParam Long districtId) {
		try {
			// Extract the username from the token (after "Bearer " prefix)
			String username = jwtUtil.extractUsername(authHeader.substring(7));

			// Fetch talukas based on the district ID
			List<IdNameDto> talukas = hadpBudgetService.getTalukasByDistrictId(districtId);

			// If no talukas found, return NO_CONTENT
			if (talukas.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body("No talukas found for the specified district.");
			}

			// Return the talukas with a successful response
			return ResponseEntity.ok(talukas);
		} catch (Exception e) {
			// Return an error response with an appropriate message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching talukas: " + e.getMessage());
		}
	}

	// Endpoint to fetch talukas based on HADP Plan Type (GROUP or SUB_GROUP)
	@GetMapping("/talukas-by-hadp-type")
	public ResponseEntity<?> getTalukasByHadpType(@RequestHeader("Authorization") String authHeader,
			@RequestParam HadpType planType) {
		try {
			// Extract the username from the token (after "Bearer " prefix)
			String username = jwtUtil.extractUsername(authHeader.substring(7));

			// Fetch talukas based on the HADP Plan Type
			List<IdNameDto> talukas = hadpBudgetService.getTalukasByHadpType(planType);

			// If no talukas found, return NO_CONTENT
			if (talukas.isEmpty()) {
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body("No talukas found for the specified HADP type.");
			}

			// Return the talukas with a successful response
			return ResponseEntity.ok(talukas);
		} catch (Exception e) {
			// Return an error response with an appropriate message
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error fetching talukas: " + e.getMessage());
		}
	}
}
