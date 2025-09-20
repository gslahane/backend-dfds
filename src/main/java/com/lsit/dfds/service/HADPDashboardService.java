// src/main/java/com/lsit/dfds/service/HADPDashboardService.java
package com.lsit.dfds.service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.BudgetByFyDto;
import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.HadpDashboardResponseDto;
import com.lsit.dfds.dto.WorkProgressItemDto;
import com.lsit.dfds.dto.WorkStatusCountDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.HADPBudget;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HADPDashboardService {

	private final UserRepository userRepository;
	private final HADPRepository hadpRepository;
	private final WorkRepository workRepository;
	private final HADPBudgetRepository hadpBudgetRepository;
	private final FundDemandRepository fundDemandRepository;

	private static double NZ(Double d) {
		return d == null ? 0.0 : d;
	}

	public HadpDashboardResponseDto getDashboard(String username, Long financialYearId, Long districtId, Long talukaId,
			Long hadpId, String search) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// Role-aware filters
		// STATE_*: can see all; optional filters
		// DISTRICT_* / IA_ADMIN: default to user's district if none provided
		// HADP_ADMIN: can see HADP data; we’ll respect provided filters, but if nothing
		// is given, and user has district, use it
		Long computedDistrict = districtId;
		if ((isDistrict(me) || isIa(me) || isHadpAdmin(me)) && computedDistrict == null) {
			if (me.getDistrict() == null) {
				throw new RuntimeException("User is not mapped to any district");
			}
			computedDistrict = me.getDistrict().getId();
		}
		final Long effDistrictId = computedDistrict; // final for lambdas

		// Gather works (HADP only)
		List<Work> works = workRepository.findWorksForHadpDashboard(hadpId, effDistrictId, talukaId, financialYearId,
				search);
		long totalWorks = works.size();

		// Status counts
		Map<WorkStatuses, Long> statusCounts = works.stream()
				.map(w -> w.getStatus() != null ? w.getStatus() : WorkStatuses.APPROVED)
				.collect(Collectors.groupingBy(java.util.function.Function.identity(),
						() -> new EnumMap<>(WorkStatuses.class), Collectors.counting()));

		List<WorkStatusCountDto> worksByStatus = Arrays.stream(WorkStatuses.values())
				.map(st -> new WorkStatusCountDto(st, statusCounts.getOrDefault(st, 0L))).toList();

		// Budgets: if hadpId is provided, prefer hadp-scoped; otherwise use filters
		List<HADPBudget> budgets;
		if (hadpId != null) {
			budgets = (financialYearId == null) ? hadpBudgetRepository.findByHadp_Id(hadpId)
					: hadpBudgetRepository.findByHadp_IdAndFinancialYear_Id(hadpId, financialYearId);
		} else if (effDistrictId != null && talukaId != null) {
			budgets = hadpBudgetRepository.findByDistrict_IdAndTaluka_Id(effDistrictId, talukaId);
		} else if (effDistrictId != null) {
			budgets = hadpBudgetRepository.findByDistrict_Id(effDistrictId);
		} else if (talukaId != null) {
			budgets = hadpBudgetRepository.findByTaluka_Id(talukaId);
		} else if (financialYearId != null) {
			budgets = hadpBudgetRepository.findByFinancialYear_Id(financialYearId);
		} else {
			budgets = hadpBudgetRepository.findAll();
		}

		double totalAllocated = budgets.stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
		double totalUtilized = budgets.stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
		double totalRemaining = totalAllocated - totalUtilized;

		// FY-wise breakdown
		Map<Long, List<HADPBudget>> byFy = budgets.stream().filter(b -> b.getFinancialYear() != null)
				.collect(Collectors.groupingBy(b -> b.getFinancialYear().getId()));

		List<BudgetByFyDto> budgetsByFy = byFy.entrySet().stream().map(e -> {
			HADPBudget any = e.getValue().get(0);
			double alloc = e.getValue().stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
			double util = e.getValue().stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
			return new BudgetByFyDto(e.getKey(), any.getFinancialYear().getFinacialYear(), alloc, util, alloc - util);
		}).sorted(Comparator.comparing(BudgetByFyDto::getFinancialYear)).toList();

		// Work progress (utilization via demands accepted by STATE or transferred)
		var accepted = List.of(DemandStatuses.APPROVED_BY_STATE, DemandStatuses.TRANSFERRED);

		List<WorkProgressItemDto> workProgress = works.stream().map(w -> {
			Double utilizedFromDemands = fundDemandRepository.sumNetPayableByWorkInStatuses(w.getId(), financialYearId,
					accepted);
			utilizedFromDemands = NZ(utilizedFromDemands);
			double aa = NZ(w.getAdminApprovedAmount());
			double progress = aa > 0 ? Math.min(100.0, (utilizedFromDemands / aa) * 100.0) : 0.0;

			// nodal is irrelevant for HADP; set false
			return new WorkProgressItemDto(w.getId(), w.getWorkName(), w.getWorkCode(), aa, utilizedFromDemands,
					progress, w.getStatus() != null ? w.getStatus().name() : "UNKNOWN", false);
		}).toList();

		// Demands table
		List<FundDemand> demands = fundDemandRepository.findDemandsForHadp(hadpId, financialYearId, effDistrictId,
				talukaId, search);

		List<FundDemandDetailDto> demandDtos = demands.stream().map(fd -> {
			Work w = fd.getWork();
			Vendor v = fd.getVendor();
			return new FundDemandDetailDto(fd.getId(), fd.getDemandId(),
					fd.getFinancialYear() != null ? fd.getFinancialYear().getId() : null,
					fd.getFinancialYear() != null ? fd.getFinancialYear().getFinacialYear() : null,
					w != null ? w.getId() : null, w != null ? w.getWorkName() : null,
					w != null ? w.getWorkCode() : null, v != null ? v.getId() : null, v != null ? v.getName() : null,
					NZ(fd.getAmount()), NZ(fd.getNetPayable()), fd.getDemandDate(),
					fd.getStatus() != null ? fd.getStatus().name() : "UNKNOWN",
					w != null ? NZ(w.getAdminApprovedAmount()) : 0.0, w != null ? NZ(w.getWorkPortionAmount()) : 0.0);
		}).toList();

		HadpDashboardResponseDto out = new HadpDashboardResponseDto();
		out.setTotalWorks(totalWorks);
		out.setWorksByStatus(worksByStatus);
		out.setTotalAllocatedBudget(totalAllocated);
		out.setTotalUtilizedBudget(totalUtilized);
		out.setTotalRemainingBudget(totalRemaining);
		out.setBudgetsByFy(budgetsByFy);
		out.setWorkProgress(workProgress);
		out.setFundDemands(demandDtos);
		return out;
	}

	private boolean isState(User u) {
		return u.getRole() == Roles.STATE_ADMIN || u.getRole() == Roles.STATE_CHECKER
				|| u.getRole() == Roles.STATE_MAKER;
	}

	private boolean isDistrict(User u) {
		return u.getRole() == Roles.DISTRICT_ADMIN || u.getRole() == Roles.DISTRICT_CHECKER
				|| u.getRole() == Roles.DISTRICT_MAKER;
	}

	private boolean isIa(User u) {
		return u.getRole() == Roles.IA_ADMIN;
	}

	private boolean isHadpAdmin(User u) {
		return u.getRole() == Roles.HADP_ADMIN;
	}
}
