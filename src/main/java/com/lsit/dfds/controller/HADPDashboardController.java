// src/main/java/com/lsit/dfds/controller/HADPDashboardController.java
package com.lsit.dfds.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.HadpDashboardResponseDto;
import com.lsit.dfds.dto.HadpTalukaDetailsDto;
import com.lsit.dfds.dto.HadpWorkSummaryDto;
import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.entity.HADPBudget;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;
import com.lsit.dfds.service.HADPDashboardService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/hadp/dashboard")
@RequiredArgsConstructor
public class HADPDashboardController {

	private final HADPDashboardService service;
	private final JwtUtil jwtUtil;

	private final HADPRepository hadpRepo;
	private final HADPBudgetRepository hadpBudgetRepo;
	private final WorkRepository workRepo;
	private final UserRepository userRepo;

	/*
	 * Role behavior: - STATE_*: may pass any filters (financialYearId, districtId,
	 * talukaId, hadpId, search) - DISTRICT_
	 *
	 * IA_ADMIN: district defaults to caller's district if not provided -
	 * HADP_ADMIN: if no filters, defaults to caller’s district (if mapped)
	 */

	@GetMapping
	public ResponseEntity<HadpDashboardResponseDto> getDashboard(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long talukaId, @RequestParam(required = false) Long hadpId,
			@RequestParam(required = false) String search) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(service.getDashboard(username, financialYearId, districtId, talukaId, hadpId, search));
	}

	/**
	 * Full HADP Taluka details: HADP row + (summed) budget + all works. Filters:
	 * districtId, talukaId, financialYearId, schemeId, type
	 */
	@GetMapping("/hadp-talukas/details")
	@Transactional
	public ResponseEntity<GenericResponseDto<List<HadpTalukaDetailsDto>>> getHadpTalukaDetails(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long talukaId, @RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) Long schemeId, @RequestParam(required = false) HadpType type) {
		String username = jwtUtil.extractUsername(auth.substring(7));
		User u = userRepo.findByUsername(username).orElse(null);

		if (districtId == null && u != null && u.getDistrict() != null) {
			districtId = u.getDistrict().getId();
		}

		// 1) Pull HADP rows by filters
		List<HADP> hadpRows = hadpRepo.findActiveHadpRows(districtId, financialYearId, schemeId, type);

		if (talukaId != null) {
			hadpRows = hadpRows.stream().filter(h -> h.getTaluka() != null && talukaId.equals(h.getTaluka().getId()))
					.collect(Collectors.toList());
		}

		// 2) Map each HADP row to a details DTO
		List<HadpTalukaDetailsDto> result = hadpRows.stream().map(h -> {
			Long hId = h.getId();
			Long dId = h.getDistrict() != null ? h.getDistrict().getId() : null;
			Long tId = h.getTaluka() != null ? h.getTaluka().getId() : null;
			Long fyId = h.getFinancialYear() != null ? h.getFinancialYear().getId() : null;
			Long scId = h.getScheme() != null ? h.getScheme().getId() : null;

			// 2a) Budgets for this HADP row (sum in case of multiple)
			List<HADPBudget> budgets = hadpBudgetRepo.findBudgets(hId, dId, tId, fyId, scId);
			double allocated = budgets.stream()
					.mapToDouble(b -> b.getAllocatedLimit() != null ? b.getAllocatedLimit() : 0.0).sum();
			double utilized = budgets.stream()
					.mapToDouble(b -> b.getUtilizedLimit() != null ? b.getUtilizedLimit() : 0.0).sum();
			double remaining = budgets.stream()
					.mapToDouble(b -> b.getRemainingLimit() != null ? b.getRemainingLimit() : 0.0).sum();

			// 2b) Works under this HADP (optionally filter by FY if supplied)
			List<Work> works = (financialYearId != null)
					? workRepo.findByHadp_IdAndFinancialYear_Id(hId, financialYearId)
					: workRepo.findByHadp_Id(hId);

			List<HadpWorkSummaryDto> workDtos = works.stream()
					.map(w -> HadpWorkSummaryDto.builder().id(w.getId()).workName(w.getWorkName())
							.workCode(w.getWorkCode()).status(w.getStatus()).recommendedAmount(w.getRecommendedAmount())
							.districtApprovedAmount(w.getDistrictApprovedAmount())
							.adminApprovedAmount(w.getAdminApprovedAmount()).balanceAmount(w.getBalanceAmount())
							.sanctionDate(w.getSanctionDate()).createdAt(w.getCreatedAt()).build())
					.collect(Collectors.toList());

			return HadpTalukaDetailsDto.builder().hadpId(hId)
					.hadpStatus(h.getStatus() != null ? h.getStatus().name() : null)
					.hadpType(h.getType() != null ? h.getType().name() : null).districtId(dId)
					.districtName(h.getDistrict() != null ? h.getDistrict().getDistrictName() : null).talukaId(tId)
					.talukaName(h.getTaluka() != null ? h.getTaluka().getTalukaName() : null).schemeId(scId)
					.schemeName(h.getScheme() != null ? h.getScheme().getSchemeName() : null).financialYearId(fyId)
					.financialYear(h.getFinancialYear() != null ? h.getFinancialYear().getFinacialYear() : null)
					.allocatedLimit(allocated).utilizedLimit(utilized).remainingLimit(remaining)
					.totalWorks(workDtos.size()).works(workDtos).build();
		}).collect(Collectors.toList());

		return ResponseEntity.ok(new GenericResponseDto<>(true, "HADP taluka details fetched", result));
	}
}
