// src/main/java/com/lsit/dfds/controller/StateDashboardController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.StateDashboardResponse;
import com.lsit.dfds.service.StateDashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/state/dashboard")
@RequiredArgsConstructor
public class StateDashboardController {

	private final StateDashboardService service;

	/**
	 * Main dashboard endpoint — returns cards + paginated table rows.
	 */
	@GetMapping
	@PreAuthorize("hasAnyRole('STATE_ADMIN','STATE_DEPT_ADMIN','STATE_FINANCE','STATE_AUDITOR')")
	public ResponseEntity<StateDashboardResponse> getDashboard(@RequestParam(name = "planType") String planType, // ML-MLA
																													// |
																													// ML-MLC
																													// |
																													// HADP
			@RequestParam(name = "financialYearId", required = false) Long fyId,
			@RequestParam(name = "districtId", required = false) Long districtId,
			@RequestParam(name = "mlaId", required = false) Long mlaId,
			@RequestParam(name = "mlcId", required = false) Long mlcId,
			@RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "showPendingOnly", required = false, defaultValue = "false") boolean showPendingOnly,
			@RequestParam(name = "page", required = false, defaultValue = "1") int page,
			@RequestParam(name = "size", required = false, defaultValue = "50") int size) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		StateDashboardResponse out = service.getDashboard(username, planType, fyId, districtId, mlaId, mlcId, search,
				showPendingOnly, page, size);
		return ResponseEntity.ok(out);
	}

	/**
	 * Click-through for pending demand details per row. type = MLA | MLC | HADP
	 * refId = MLA.id or MLC.id or TALUKA.id (or HADP.id if you choose)
	 */
	@GetMapping("/pending-demands")
	@PreAuthorize("hasAnyRole('STATE_ADMIN','STATE_DEPT_ADMIN','STATE_FINANCE','STATE_AUDITOR')")
	public ResponseEntity<List<FundDemandDetailDto>> getPendingDemandsForRow(@RequestParam(name = "type") String type,
			@RequestParam(name = "refId") Long refId,
			@RequestParam(name = "financialYearId", required = false) Long fyId,
			@RequestParam(name = "districtId", required = false) Long districtId,
			@RequestParam(name = "search", required = false) String search,
			@RequestParam(name = "page", defaultValue = "1") int page,
			@RequestParam(name = "size", defaultValue = "50") int size) {
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		var list = service.getPendingDemands(username, type, refId, fyId, districtId, search, page, size);
		return ResponseEntity.ok(list);
	}
}
