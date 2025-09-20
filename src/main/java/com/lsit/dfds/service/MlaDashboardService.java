package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.ApiResponse;
import com.lsit.dfds.dto.BudgetByFyDto;
import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.MlaDashboardFilterDto;
import com.lsit.dfds.dto.MlaDashboardResponseDto;
import com.lsit.dfds.dto.WorkProgressItemDto;
import com.lsit.dfds.dto.WorkStatusCountDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MlaDashboardService {

	private final UserRepository userRepository;
	private final MLARepository mlaRepository;
	private final WorkRepository workRepository;
	private final FundDemandRepository fundDemandRepository;
	private final MLABudgetRepository mlaBudgetRepository;
	private final MLATermRepository mlaTermRepository;

	// ================= MLA-SELF DASHBOARD =================

	@Transactional(readOnly = true)
	public ApiResponse<MlaDashboardResponseDto> getMyDashboard(String username, Long financialYearId) {

		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (user.getRole() != Roles.MLA) {
			return ApiResponse.error("Forbidden: Only MLA can access this endpoint.");
		}
		MLA mla = mlaRepository.findByUser_Id(user.getId())
				.orElseThrow(() -> new RuntimeException("MLA profile not linked to user"));

		return ApiResponse.ok(buildDashboardForMla(mla.getId(), financialYearId, null, null, null));
	}

	// ================= STATE VIEW (FILTERABLE) =================

	@Transactional(readOnly = true)
	public ApiResponse<MlaDashboardResponseDto> getDashboardForState(MlaDashboardFilterDto filter) {
		Long mlaId = filter.getMlaId();
		Long fyId = filter.getFinancialYearId();
		Long districtId = filter.getDistrictId();
		Long constituencyId = filter.getConstituencyId();
		String search = norm(filter.getSearch());

		return ApiResponse.ok(buildDashboardForMla(mlaId, fyId, districtId, constituencyId, search));
	}

	// ================= CORE BUILDER =================
	private MlaDashboardResponseDto buildDashboardForMla(Long mlaId, Long financialYearId, Long districtId,
			Long constituencyId, String search) {

		// -------- Works (filtered) --------
		List<Work> works = workRepository.findWorksForMlaDashboard(mlaId, districtId, constituencyId, search);
		List<Work> sortedWorks = works.stream()
				.filter(w -> w.getFinancialYear() != null && w.getFinancialYear().getId().equals(financialYearId))
				.collect(Collectors.toList());
		long totalWorks = sortedWorks.size();

		// Status counts
		Map<WorkStatuses, Long> statusCounts = sortedWorks.stream().map(Work::getStatus).filter(Objects::nonNull)
				.collect(Collectors.groupingBy(java.util.function.Function.identity(),
						() -> new java.util.EnumMap<>(WorkStatuses.class), Collectors.counting()));

		List<WorkStatusCountDto> worksByStatus = Arrays.stream(WorkStatuses.values())
				.map(st -> new WorkStatusCountDto(st, statusCounts.getOrDefault(st, 0L))).toList();

		// -------- Budgets (MLABudget) --------
		List<MLABudget> budgets = (mlaId == null) ? Collections.emptyList()
				: (financialYearId == null ? mlaBudgetRepository.findByMla_Id(mlaId)
						: mlaBudgetRepository.findByMla_IdAndFinancialYear_Id(mlaId, financialYearId));

		double totalAllocated = budgets.stream().mapToDouble(b -> nz(b.getAllocatedLimit())).sum();
		double totalUtilized = budgets.stream().mapToDouble(b -> nz(b.getUtilizedLimit())).sum();
		double totalRemaining = totalAllocated - totalUtilized;

		// FY-wise breakdown (for trend card)
		Map<Long, List<MLABudget>> byFy = budgets.stream().filter(b -> b.getFinancialYear() != null)
				.collect(Collectors.groupingBy(b -> b.getFinancialYear().getId()));

		List<BudgetByFyDto> budgetsByFy = byFy.entrySet().stream().map(e -> {
			var any = e.getValue().get(0);
			double alloc = e.getValue().stream().mapToDouble(b -> nz(b.getAllocatedLimit())).sum();
			double util = e.getValue().stream().mapToDouble(b -> nz(b.getUtilizedLimit())).sum();
			return new BudgetByFyDto(e.getKey(), any.getFinancialYear().getFinacialYear(), alloc, util, alloc - util);
		}).sorted(Comparator.comparing(BudgetByFyDto::getFinancialYear)).toList();

		// Current FY sanctioned/remaining/carry-forward (cards)
		double currentFySanctioned = 0.0;
		double currentFyRemaining = 0.0;
		double lastYearSpill = 0.0;
		if (financialYearId != null) {
			List<MLABudget> curr = mlaBudgetRepository.findByMla_IdAndFinancialYear_Id(mlaId, financialYearId);
			currentFySanctioned = curr.stream().mapToDouble(b -> nz(b.getAllocatedLimit())).sum();
			double currUtil = curr.stream().mapToDouble(b -> nz(b.getUtilizedLimit())).sum();
			currentFyRemaining = currentFySanctioned - currUtil;
			// carryForwardIn represents spill brought into this FY
			lastYearSpill = curr.stream().mapToDouble(b -> nz(b.getCarryForwardIn())).sum();
		}

		// -------- Recommended vs Sanctioned (cards) --------
		// Recommended count/cost (use grossAmount; fallback adminApprovedAmount if
		// gross not set)
		long totalRecommendedCount = sortedWorks.size();
		double totalRecommendedCost = sortedWorks.stream()
				.mapToDouble(w -> nz(w.getGrossAmount()) > 0 ? nz(w.getGrossAmount()) : nz(w.getAdminApprovedAmount()))
				.sum();

		// Sanctioned = AA_APPROVED or later
		Set<WorkStatuses> aaOrLater = EnumSet.of(WorkStatuses.AA_APPROVED, WorkStatuses.WORK_ORDER_ISSUED,
				WorkStatuses.IN_PROGRESS, WorkStatuses.COMPLETED);
		List<Work> aaWorks = works.stream().filter(w -> w.getStatus() != null && aaOrLater.contains(w.getStatus()))
				.toList();
		long totalSanctionedCount = aaWorks.size();
		double totalSanctionedCost = aaWorks.stream().mapToDouble(w -> nz(w.getAdminApprovedAmount())).sum();

		// Permissible scope = remaining budget (you can refine policy later if needed)
//		double permissibleScope = currentFyRemaining;

		// -------- Work progress (via demands approved or transferred) --------
		List<WorkProgressItemDto> workProgress = sortedWorks.stream().map(w -> {
			Double utilizedFromDemands = fundDemandRepository.sumApprovedNetPayableByWork(w.getId(), financialYearId);
			utilizedFromDemands = utilizedFromDemands == null ? 0.0 : utilizedFromDemands;
			double aa = nz(w.getAdminApprovedAmount());
			double progress = aa > 0 ? Math.min(100.0, (utilizedFromDemands / aa) * 100.0) : 0.0;

			return new WorkProgressItemDto(w.getId(), w.getWorkName(), w.getWorkCode(), aa, utilizedFromDemands,
					progress, w.getStatus() != null ? w.getStatus().name() : "UNKNOWN");
		}).toList();

		// -------- Demands table --------
		List<FundDemand> demands = (mlaId == null) ? Collections.emptyList()
				: fundDemandRepository.findDemandsForMla(mlaId, financialYearId, districtId, constituencyId, search);

		// Total fund disbursed (for the header/right corner in your grid)
		double totalFundDisbursed = demands.stream().filter(fd -> fd.getStatus() == DemandStatuses.TRANSFERRED)
				.mapToDouble(fd -> nz(fd.getNetPayable())).sum();

		List<FundDemandDetailDto> demandDtos = demands.stream().map(fd -> {
			Work w = fd.getWork();
			Vendor v = fd.getVendor();
			return new FundDemandDetailDto(fd.getId(), fd.getDemandId(),
					fd.getFinancialYear() != null ? fd.getFinancialYear().getId() : null,
					fd.getFinancialYear() != null ? fd.getFinancialYear().getFinacialYear() : null,
					w != null ? w.getId() : null, w != null ? w.getWorkName() : null,
					w != null ? w.getWorkCode() : null, v != null ? v.getId() : null, v != null ? v.getName() : null,
					nz(fd.getAmount()), nz(fd.getNetPayable()), fd.getDemandDate(),
					fd.getStatus() != null ? fd.getStatus().name() : "UNKNOWN",
					w != null ? nz(w.getAdminApprovedAmount()) : 0.0, w != null ? nz(w.getWorkPortionAmount()) : 0.0);
		}).toList();

		// -------- Work table (for the big grid under cards) --------
		// Columns: Sr/Work, IA, MLA Letter, Recommended Amount, AA Amount, AA Letter,
		// Fund Disbursed, Status
		// NOTE: We don’t have MLA letter no. explicitly; using
		// remark/adminApprovedletterUrl as best-effort.
		Map<Long, Double> disbursedByWork = demands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.TRANSFERRED && d.getWork() != null).collect(Collectors
						.groupingBy(d -> d.getWork().getId(), Collectors.summingDouble(d -> nz(d.getNetPayable()))));

		List<Map<String, Object>> worksTable = new ArrayList<>();
		int[] sr = { 1 };
		for (Work w : sortedWorks) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("srNo", sr[0]++);
			row.put("workId", w.getId());
			row.put("workCode", w.getWorkCode());
			row.put("workName", w.getWorkName());
			row.put("iaName", w.getImplementingAgency() != null ? w.getImplementingAgency().getFullname() : null);
			row.put("mlaLetter", w.getRecommendationLetterUrl()); // placeholder; replace if you store letter no. separately
			row.put("recommendedAmount",w.getRecommendedAmount());
			row.put("grossAmount", w.getGrossAmount());
			row.put("WorkOrderLetter", w.getWorkOrderLetterUrl());
			row.put("aaAmount", nz(w.getAdminApprovedAmount()));
			row.put("aaLetterUrl", w.getAdminApprovedletterUrl());
			row.put("fundDisbursed", disbursedByWork.getOrDefault(w.getId(), 0.0));
			row.put("status", w.getStatus() != null ? w.getStatus().name() : "UNKNOWN");
			worksTable.add(row);
		}

		// -------- Assemble response --------
		MlaDashboardResponseDto out = new MlaDashboardResponseDto();
		out.setTotalWorks(totalWorks);
		out.setWorksByStatus(worksByStatus);
		out.setTotalAllocatedBudget(totalAllocated);
		out.setTotalUtilizedBudget(totalUtilized);
		out.setTotalRemainingBudget(totalRemaining);
		out.setBudgetsByFy(budgetsByFy);
		out.setWorkProgress(workProgress);
		out.setFundDemands(demandDtos);

		// new cards
		out.setCurrentFySanctioned(currentFySanctioned);
		out.setCurrentFyRemaining(currentFyRemaining);
		out.setLastYearSpill(lastYearSpill);
		out.setTotalWorksRecommendedCount(totalRecommendedCount);
		out.setTotalWorksRecommendedCost(totalRecommendedCost);
		out.setTotalWorksSanctionedCount(totalSanctionedCount);
		out.setTotalWorksSanctionedCost(totalSanctionedCost);
//		out.setPermissibleScope(permissibleScope);
		out.setTotalFundDisbursed(totalFundDisbursed);
		out.setWorksTable(worksTable);

		// -------- MLA Term Details --------
		if (mlaId != null) {
			MLATerm mlaTerm = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mlaId).orElse(null);
			if (mlaTerm != null) {
				out.setMlaTermStartDate(
						mlaTerm.getTenureStartDate() != null ? mlaTerm.getTenureStartDate().toString() : null);
				out.setMlaTermEndDate(
						mlaTerm.getTenureEndDate() != null ? mlaTerm.getTenureEndDate().toString() : null);
				out.setMlaTermNumber(mlaTerm.getTermNumber());
				out.setMlaTenurePeriod(mlaTerm.getTenurePeriod());

				// Permissible scope logic for MLA term
				if (mlaTerm.getTenureEndDate() != null) {
					java.time.LocalDate today = java.time.LocalDate.now(); // use system date
					long yearsAhead = java.time.temporal.ChronoUnit.YEARS.between(today, mlaTerm.getTenureEndDate());
					if (yearsAhead >= 1) {
						out.setMlaPermissibleScope((totalAllocated * 1.5) - totalUtilized);
						out.setPermissibleScope(totalAllocated * 1.5);
					} else {
						out.setMlaPermissibleScope(totalAllocated - totalUtilized);

						out.setPermissibleScope(totalAllocated);

					}
				} else {
					out.setMlaPermissibleScope(totalAllocated - totalUtilized);
					out.setPermissibleScope(totalAllocated);

				}
			}
		}

		return out;
	}

	private String norm(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}

	private double nz(Double d) {
		return d == null ? 0.0 : d;
	}

}