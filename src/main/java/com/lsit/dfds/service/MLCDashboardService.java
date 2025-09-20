// src/main/java/com/lsit/dfds/service/MLCDashboardService.java
package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.BudgetByFyDto;
import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.MlcDashboardResponseDto;
import com.lsit.dfds.dto.WorkProgressItemDto;
import com.lsit.dfds.dto.WorkStatusCountDto;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.MLCTerm;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.MLCTermRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MLCDashboardService {

	private final UserRepository userRepository;
	private final MLCRepository mlcRepository;
	private final WorkRepository workRepository;
	private final MLCBudgetRepository mlcBudgetRepository;
	private final FundDemandRepository fundDemandRepository;
	private final MLCTermRepository mlcTermRepository;

	private static double NZ(Double d) {
		return d == null ? 0.0 : d;
	}

	private static boolean isState(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private static boolean isDistrict(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("DISTRICT");
	}

	private static boolean isIa(User u) {
		return u.getRole() == Roles.IA_ADMIN;
	}

	private static boolean isMlc(User u) {
		return u.getRole() == Roles.MLC;
	}

	public MlcDashboardResponseDto getDashboard(String username, Long financialYearId, Long districtId, Long mlcId,
			String search, Boolean nodalFilter, Integer page, Integer size) {
		// 1) Caller + role constraints
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long effDistrictId;
		if ((isDistrict(me) || isIa(me)) && districtId == null) {
			if (me.getDistrict() == null) {
				throw new RuntimeException("User is not mapped to any district");
			}
			effDistrictId = me.getDistrict().getId();
		} else {
			effDistrictId = districtId;
		}

		// 2) Determine which MLC(s) to include
		List<Long> effectiveMlcIds = new ArrayList<>();
		MLC myMlc = null;

		if (isMlc(me)) {
			myMlc = mlcRepository.findByUser_Id(me.getId())
					.orElseThrow(() -> new RuntimeException("MLC record not linked to user"));
			effectiveMlcIds.add(myMlc.getId());

		} else if (isState(me)) {
			if (mlcId != null) {
				effectiveMlcIds.add(mlcId);
			} else {
				List<MLC> all = mlcRepository.findAll();
				if (effDistrictId == null) {
					effectiveMlcIds = all.stream().map(MLC::getId).collect(Collectors.toList());
				} else {
					Long dId = effDistrictId;
					effectiveMlcIds = all.stream().filter(m -> hasDistrict(m, dId)).map(MLC::getId)
							.collect(Collectors.toList());
				}
			}

		} else if (isDistrict(me) || isIa(me)) {
			if (mlcId == null) {
				throw new RuntimeException("mlcId is required for District/IA dashboard view");
			}
			effectiveMlcIds.add(mlcId);

		} else {
			throw new RuntimeException("Role not allowed for MLC dashboard");
		}

		if (effectiveMlcIds.isEmpty()) {
			return emptyResponse();
		}

		// 3) Load ALL relevant works for those MLCs (unpaged), then filter in-service
		List<Work> works = new ArrayList<>();
		for (Long id : effectiveMlcIds) {
			// Reliable base fetch by relation
			works.addAll(workRepository.findByRecommendedByMlc_Id(id));
		}

		// De-dup (if same work appears multiple times due to joins elsewhere)
		Map<Long, Work> uniq = new LinkedHashMap<>();
		for (Work w : works) {
			uniq.put(w.getId(), w);
		}
		works = new ArrayList<>(uniq.values());

		// Apply filters: district, FY (with fallback to scheme.FY), search, nodal
		final Long fyFilter = financialYearId;
		final Long districtFilter = effDistrictId;
		final String q = (search != null ? search.trim().toLowerCase() : null);

		works = works.stream()
				.filter(w -> districtFilter == null
						|| (w.getDistrict() != null && Objects.equals(w.getDistrict().getId(), districtFilter)))
				.filter(w -> {
					if (fyFilter == null)
						return true;
					Long wf = getWorkFyId(w);
					return wf != null && Objects.equals(wf, fyFilter);
				}).filter(w -> {
					if (q == null || q.isEmpty())
						return true;
					String name = w.getWorkName() != null ? w.getWorkName().toLowerCase() : "";
					String code = w.getWorkCode() != null ? w.getWorkCode().toLowerCase() : "";
					return name.contains(q) || code.contains(q);
				}).collect(Collectors.toList());

		if (nodalFilter != null) {
			boolean want = nodalFilter.booleanValue();
			works = works.stream().filter(w -> Boolean.TRUE.equals(w.getIsNodalWork()) == want)
					.collect(Collectors.toList());
		}

		// 4) Aggregates from FULL filtered set (not paged!)
		long totalWorks = works.size();
		long nodalCount = works.stream().filter(w -> Boolean.TRUE.equals(w.getIsNodalWork())).count();
		long nonNodalCount = totalWorks - nodalCount;

		Map<WorkStatuses, Long> statusCounts = works.stream()
				.map(w -> w.getStatus() != null ? w.getStatus() : WorkStatuses.PROPOSED)
				.collect(Collectors.groupingBy(java.util.function.Function.identity(),
						() -> new EnumMap<>(WorkStatuses.class), Collectors.counting()));

		List<WorkStatusCountDto> worksByStatus = Arrays.stream(WorkStatuses.values())
				.map(st -> new WorkStatusCountDto(st, statusCounts.getOrDefault(st, 0L))).collect(Collectors.toList());

		// 5) Budgets for MLC(s)
		List<MLCBudget> budgets = new ArrayList<>();
		for (Long id : effectiveMlcIds) {
			budgets.addAll(fyFilter == null ? mlcBudgetRepository.findByMlc_Id(id)
					: mlcBudgetRepository.findByMlc_IdAndFinancialYear_Id(id, fyFilter));
		}

		double totalAllocated = budgets.stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
		double totalUtilized = budgets.stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
		double totalRemaining = totalAllocated - totalUtilized;

		Map<Long, List<MLCBudget>> byFy = budgets.stream().filter(b -> b.getFinancialYear() != null)
				.collect(Collectors.groupingBy(b -> b.getFinancialYear().getId()));

		List<BudgetByFyDto> budgetsByFy = byFy.entrySet().stream().map(e -> {
			MLCBudget any = e.getValue().get(0);
			double alloc = e.getValue().stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
			double util = e.getValue().stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
			return new BudgetByFyDto(e.getKey(),
					any.getFinancialYear() != null ? any.getFinancialYear().getFinacialYear() : null, alloc, util,
					alloc - util);
		}).sorted(
				Comparator.comparing(BudgetByFyDto::getFinancialYear, Comparator.nullsLast(Comparator.naturalOrder())))
				.collect(Collectors.toList());

		// Current FY cards
		double currentFySanctioned = 0.0;
		double currentFyRemaining = 0.0;
		double lastYearSpill = 0.0;
		if (fyFilter != null) {
			List<MLCBudget> curr = budgets.stream()
					.filter(b -> b.getFinancialYear() != null && Objects.equals(b.getFinancialYear().getId(), fyFilter))
					.collect(Collectors.toList());
			currentFySanctioned = curr.stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
			double currUtil = curr.stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
			currentFyRemaining = currentFySanctioned - currUtil;
			lastYearSpill = curr.stream().mapToDouble(b -> NZ(b.getCarryForwardIn())).sum();
		}
		double permissibleScope = currentFyRemaining;

		// Recommended vs Sanctioned (AA+)
		long totalRecommendedCount = totalWorks;
		double totalRecommendedCost = works.stream().mapToDouble(w -> {
			double g = NZ(w.getGrossAmount());
			double aa = NZ(w.getAdminApprovedAmount());
			return g > 0 ? g : aa;
		}).sum();

		Set<WorkStatuses> aaOrLater = EnumSet.of(WorkStatuses.AA_APPROVED, WorkStatuses.WORK_ORDER_ISSUED,
				WorkStatuses.IN_PROGRESS, WorkStatuses.COMPLETED);

		List<Work> aaWorks = works.stream().filter(w -> w.getStatus() != null && aaOrLater.contains(w.getStatus()))
				.collect(Collectors.toList());

		long totalSanctionedCount = aaWorks.size();
		double totalSanctionedCost = aaWorks.stream().mapToDouble(w -> NZ(w.getAdminApprovedAmount())).sum();

		// 6) Demands based on the filtered work set (consistent with dashboard)
		List<Long> workIds = works.stream().map(Work::getId).collect(Collectors.toList());
		List<FundDemand> demands = new ArrayList<>();
		for (Long wid : workIds) {
			demands.addAll(fundDemandRepository.findByWork_Id(wid));
		}

		// Apply FY and nodal filters to demands as well (align with works)
		if (fyFilter != null) {
			demands = demands.stream().filter(fd -> {
				FinancialYear fy = fd.getFinancialYear();
				return fy != null && Objects.equals(fy.getId(), fyFilter);
			}).collect(Collectors.toList());
		}
		if (nodalFilter != null) {
			boolean want = nodalFilter.booleanValue();
			demands = demands.stream()
					.filter(d -> d.getWork() != null && Boolean.TRUE.equals(d.getWork().getIsNodalWork()) == want)
					.collect(Collectors.toList());
		}

		double totalFundDisbursed = demands.stream().filter(fd -> fd.getStatus() == DemandStatuses.TRANSFERRED)
				.mapToDouble(fd -> NZ(fd.getNetPayable())).sum();

		// Work progress from demands
		var acceptedStatuses = Arrays.asList(DemandStatuses.APPROVED_BY_STATE, DemandStatuses.TRANSFERRED);
		Map<Long, Double> utilizedByWork = demands.stream()
				.filter(fd -> fd.getWork() != null && acceptedStatuses.contains(fd.getStatus()))
				.collect(Collectors.groupingBy(fd -> fd.getWork().getId(),
						Collectors.summingDouble(fd -> NZ(fd.getNetPayable()))));

		List<WorkProgressItemDto> workProgress = works.stream().map(w -> {
			double aa = NZ(w.getAdminApprovedAmount());
			double utilized = utilizedByWork.getOrDefault(w.getId(), 0.0);
			double prog = aa > 0 ? Math.min(100.0, (utilized / aa) * 100.0) : 0.0;
			return new WorkProgressItemDto(w.getId(), w.getWorkName(), w.getWorkCode(), aa, utilized, prog,
					w.getStatus() != null ? w.getStatus().name() : "UNKNOWN", Boolean.TRUE.equals(w.getIsNodalWork()));
		}).collect(Collectors.toList());

		// Demand DTOs
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
		}).collect(Collectors.toList());

		// 7) Works table (paginate here only)
		List<Map<String, Object>> worksTable = new ArrayList<>();
		// Sort newest first (createdAt nulls last)
		works.sort(
				Comparator.comparing(Work::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

		int p = page != null && page >= 0 ? page : 0;
		int s = size != null && size > 0 ? size : 10;
		int from = Math.min(p * s, works.size());
		int to = Math.min(from + s, works.size());
		List<Work> pageSlice = works.subList(from, to);

		Map<Long, Double> disbursedByWork = demands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.TRANSFERRED && d.getWork() != null).collect(Collectors
						.groupingBy(d -> d.getWork().getId(), Collectors.summingDouble(d -> NZ(d.getNetPayable()))));

		int sr = 1 + from;
		for (Work w : pageSlice) {
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("srNo", sr++);
			row.put("workId", w.getId());
			row.put("workCode", w.getWorkCode());
			row.put("workName", w.getWorkName());
			row.put("iaName", w.getImplementingAgency() != null ? w.getImplementingAgency().getFullname() : null);
			row.put("nodal", Boolean.TRUE.equals(w.getIsNodalWork()));
			row.put("recommendedAmount", NZ(w.getRecommendedAmount()));
			row.put("districtApprovedAmount", NZ(w.getDistrictApprovedAmount()));
			row.put("adminApprovedAmount", NZ(w.getAdminApprovedAmount()));
			row.put("grossAmount", NZ(w.getGrossAmount()));
			row.put("balanceAmount", NZ(w.getBalanceAmount()));
			row.put("aaAmount", NZ(w.getAdminApprovedAmount()));
			row.put("aaLetterUrl", w.getAdminApprovedletterUrl());
			row.put("fundDisbursed", disbursedByWork.getOrDefault(w.getId(), 0.0));
			row.put("status", w.getStatus() != null ? w.getStatus().name() : "PENDING");
			row.put("createdAt", w.getCreatedAt());
			row.put("schemeName", w.getScheme() != null ? w.getScheme().getSchemeName() : null);
			row.put("districtName", w.getDistrict() != null ? w.getDistrict().getDistrictName() : null);
			row.put("financialYear", getWorkFyStr(w));
			worksTable.add(row);
		}

		// 8) Assemble response
		MlcDashboardResponseDto out = new MlcDashboardResponseDto();
		out.setTotalWorks(totalWorks);
		out.setNodalWorks(nodalCount);
		out.setNonNodalWorks(nonNodalCount);
		out.setWorksByStatus(worksByStatus);

		out.setTotalAllocatedBudget(totalAllocated);
		out.setTotalUtilizedBudget(totalUtilized);
		out.setTotalRemainingBudget(totalRemaining);
		out.setBudgetsByFy(budgetsByFy);

		out.setWorkProgress(workProgress);
		out.setFundDemands(demandDtos);

		out.setCurrentFySanctioned(currentFySanctioned);
		out.setCurrentFyRemaining(currentFyRemaining);
		out.setLastYearSpill(lastYearSpill);
		out.setTotalWorksRecommendedCount(totalRecommendedCount);
		out.setTotalWorksRecommendedCost(totalRecommendedCost);
		out.setTotalWorksSanctionedCount(totalSanctionedCount);
		out.setTotalWorksSanctionedCost(totalSanctionedCost);
		out.setPermissibleScope(permissibleScope);
		out.setTotalFundDisbursed(totalFundDisbursed);
		out.setWorksTable(worksTable);

		// 9) MLC Term cards
		if (myMlc != null) {
			MLCTerm mlcTerm = mlcTermRepository.findFirstByMlc_IdAndActiveTrue(myMlc.getId()).orElse(null);
			if (mlcTerm != null) {
				out.setMlcTermStartDate(
						mlcTerm.getTenureStartDate() != null ? mlcTerm.getTenureStartDate().toString() : null);
				out.setMlcTermEndDate(
						mlcTerm.getTenureEndDate() != null ? mlcTerm.getTenureEndDate().toString() : null);
				out.setMlcTermNumber(mlcTerm.getTermNumber());
				out.setMlcTenurePeriod(mlcTerm.getTenurePeriod());

				// A simple permissible-scope projection across tenure
				if (mlcTerm.getTenureEndDate() != null) {
					java.time.LocalDate today = java.time.LocalDate.now();
					long yearsAhead = java.time.temporal.ChronoUnit.YEARS.between(today, mlcTerm.getTenureEndDate());
					if (yearsAhead >= 1) {
						out.setMlcPermissibleScope((totalAllocated * 1.5) - totalUtilized);
						out.setPermissibleScope(totalAllocated * 1.5);
					} else {
						out.setMlcPermissibleScope(totalAllocated - totalUtilized);
						out.setPermissibleScope(totalAllocated);
					}
				} else {
					out.setMlcPermissibleScope(totalAllocated - totalUtilized);
					out.setPermissibleScope(totalAllocated);
				}
			}
		}

		return out;
	}

	// ---------- helpers ----------

	private static boolean hasDistrict(MLC mlc, Long districtId) {
		if (districtId == null)
			return true;
		if (mlc.getAccessibleDistricts() != null) {
			return mlc.getAccessibleDistricts().stream().anyMatch(d -> Objects.equals(d.getId(), districtId));
		}
		return false;
	}

	private static Long getWorkFyId(Work w) {
		if (w.getFinancialYear() != null) {
			return w.getFinancialYear().getId();
		}
		if (w.getScheme() != null && w.getScheme().getFinancialYear() != null) {
			return w.getScheme().getFinancialYear().getId();
		}
		return null;
	}

	private static String getWorkFyStr(Work w) {
		if (w.getFinancialYear() != null && w.getFinancialYear().getFinacialYear() != null) {
			return w.getFinancialYear().getFinacialYear();
		}
		if (w.getScheme() != null && w.getScheme().getFinancialYear() != null) {
			return w.getScheme().getFinancialYear().getFinacialYear();
		}
		return null;
	}

	private static MlcDashboardResponseDto emptyResponse() {
		MlcDashboardResponseDto out = new MlcDashboardResponseDto();
		out.setTotalWorks(0L);
		out.setNodalWorks(0L);
		out.setNonNodalWorks(0L);
		out.setWorksByStatus(Arrays.stream(WorkStatuses.values()).map(ws -> new WorkStatusCountDto(ws, 0L))
				.collect(Collectors.toList()));
		out.setTotalAllocatedBudget(0.0);
		out.setTotalUtilizedBudget(0.0);
		out.setTotalRemainingBudget(0.0);
		out.setBudgetsByFy(new ArrayList<>());
		out.setWorkProgress(new ArrayList<>());
		out.setFundDemands(new ArrayList<>());
		out.setCurrentFySanctioned(0.0);
		out.setCurrentFyRemaining(0.0);
		out.setLastYearSpill(0.0);
		out.setTotalWorksRecommendedCount(0L);
		out.setTotalWorksRecommendedCost(0.0);
		out.setTotalWorksSanctionedCount(0L);
		out.setTotalWorksSanctionedCost(0.0);
		out.setPermissibleScope(0.0);
		out.setTotalFundDisbursed(0.0);
		out.setWorksTable(new ArrayList<>());
		return out;
	}
}
