// MlaDashboardController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.ApiResponse;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.MlaDashboardFilterDto;
import com.lsit.dfds.dto.MlaDashboardResponseDto;
import com.lsit.dfds.service.MLAServiceImpl;
import com.lsit.dfds.service.MlaDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mla/dashboard")
@RequiredArgsConstructor
public class MlaDashboardController {

	private final MlaDashboardService dashboardService;
	private final JwtUtil jwtUtil;
	private final MLAServiceImpl mlaService;

	// MLA self view
	@GetMapping
	public ResponseEntity<ApiResponse<MlaDashboardResponseDto>> getMyDashboard(
			@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Long financialYearId) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(dashboardService.getMyDashboard(username, financialYearId));
	}

	// STATE view with filters (FY, District, Constituency, MLA, Search)
	@GetMapping("/stateView")
	public ResponseEntity<ApiResponse<MlaDashboardResponseDto>> getDashboardForState(
			@RequestHeader("Authorization") String authHeader, @RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long constituencyId,
			@RequestParam(required = false) Long mlaId, @RequestParam(required = false) String search) {

		// Optionally verify role is STATE_* here if you prefer:
		jwtUtil.extractUsername(authHeader.substring(7));

		var filter = new MlaDashboardFilterDto(financialYearId, districtId, constituencyId, mlaId, search);
		return ResponseEntity.ok(dashboardService.getDashboardForState(filter));
	}

	// Controller snippet
	@GetMapping("/mla/by-district/{districtId}")
	public ResponseEntity<List<IdNameDto>> getMlaByDistrict(@PathVariable Long districtId) {
		return ResponseEntity.ok(mlaService.getMlaDropdownByDistrict(districtId));
	}
}
