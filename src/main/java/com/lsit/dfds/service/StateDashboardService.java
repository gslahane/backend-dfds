// src/main/java/com/lsit/dfds/service/StateDashboardService.java
package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.StateDashboardResponse;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StateDashboardService {

	private final UserRepository userRepo;
	private final MLABudgetRepository mlaBudgetRepo;
	private final MLCBudgetRepository mlcBudgetRepo;
	private final FundDemandRepository fundDemandRepo;
	private final MLARepository mlaRepo;
	private final MLCRepository mlcRepo;
	private final TalukaRepository talukaRepo;
	private final HADPBudgetRepository hadpBudgetRepo; // optional if you maintain HADP budgets
	private final WorkRepository workRepo;

	private static double NZ(Double d) {
		return d == null ? 0.0 : d;
	}

	private static boolean isState(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	public StateDashboardResponse getDashboard(String username, String planType, Long fyId, Long districtId, Long mlaId,
			Long mlcId, String search, boolean showPendingOnly, int page, int size) {
		User me = userRepo.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		if (!isState(me)) {
			throw new RuntimeException("Forbidden: only STATE_* roles can view this dashboard");
		}

		StateDashboardResponse out = new StateDashboardResponse();
		out.setPlanType(planType);
		out.setFinancialYearId(fyId);
		out.setDistrictId(districtId);
		out.setSearch(search);
		out.setShowPendingOnly(showPendingOnly);
		out.setPage(page);
		out.setSize(size);

		// Overview sums
		out.setTotalMlaFund(mlaBudgetRepo.sumAllocatedByFyNullable(fyId));
		out.setTotalMlcFund(mlcBudgetRepo.sumAllocatedByFyNullable(fyId));
		out.setTotalHadpFund(hadpBudgetRepo.sumAllocatedByFyNullable(fyId)); // 0.0 if not maintained

		switch (planType) {
		case "ML-MLA" -> fillMla(out, fyId, districtId, mlaId, search, showPendingOnly, page, size);
		case "ML-MLC" -> fillMlc(out, fyId, districtId, mlcId, search, showPendingOnly, page, size);
		case "HADP" -> fillHadp(out, fyId, districtId, search, showPendingOnly, page, size);
		default -> throw new IllegalArgumentException("Invalid planType: " + planType);
		}

		return out;
	}

	// ---------- MLA ------------

	private void fillMla(StateDashboardResponse out, Long fyId, Long districtId, Long mlaId, String search,
			boolean showPendingOnly, int page, int size) {

		// Pull budgets (filter by FY & District if provided)
		List<MLABudget> budgets = (fyId == null) ? mlaBudgetRepo.findAllActive()
				: mlaBudgetRepo.findByFinancialYear_Id(fyId);

		// Restrict by district
		if (districtId != null) {
			budgets = budgets.stream()
					.filter(b -> b.getConstituency() != null && b.getConstituency().getDistrict() != null
							&& Objects.equals(b.getConstituency().getDistrict().getId(), districtId))
					.toList();
		}

		// Restrict by MLA
		if (mlaId != null) {
			budgets = budgets.stream().filter(b -> b.getMla() != null && Objects.equals(b.getMla().getId(), mlaId))
					.toList();
		}

		// Group by MLA
		Map<Long, List<MLABudget>> byMla = budgets.stream().filter(b -> b.getMla() != null)
				.collect(Collectors.groupingBy(b -> b.getMla().getId(), LinkedHashMap::new, Collectors.toList()));

		List<StateDashboardResponse.MlaRow> rows = new ArrayList<>();

		for (var e : byMla.entrySet()) {
			MLA mla = e.getValue().get(0).getMla();
			var any = e.getValue().get(0);

			String name = Optional.ofNullable(mla.getMlaName()).orElse("");
			String constituencyName = (any.getConstituency() != null) ? any.getConstituency().getConstituencyName()
					: null;
			Long dId = (any.getConstituency() != null && any.getConstituency().getDistrict() != null)
					? any.getConstituency().getDistrict().getId()
					: null;
			String dName = (any.getConstituency() != null && any.getConstituency().getDistrict() != null)
					? any.getConstituency().getDistrict().getDistrictName()
					: null;

			// aggregate budget/utilized
			double budget = e.getValue().stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
			double utilized = e.getValue().stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
			double balance = budget - utilized;

			// pending: PENDING + APPROVED_BY_STATE (not yet transferred)
			double pendingAmt = (fyId != null)
					? NZ(fundDemandRepo.sumByMlaAndFyInStatuses(mla.getId(), fyId,
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE)))
					: NZ(fundDemandRepo.sumByMlaInStatuses(mla.getId(),
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE)));

			long pendingCount = (fyId != null)
					? fundDemandRepo.countByMlaAndFyInStatuses(mla.getId(), fyId,
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE))
					: fundDemandRepo.countByMlaInStatuses(mla.getId(),
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE));

			// free-text search
			if (search != null && !search.isBlank()) {
				String s = search.toLowerCase();
				boolean hit = name.toLowerCase().contains(s)
						|| (constituencyName != null && constituencyName.toLowerCase().contains(s))
						|| (dName != null && dName.toLowerCase().contains(s));
				if (!hit)
					continue;
			}

			if (showPendingOnly && pendingAmt <= 0)
				continue;

			rows.add(new StateDashboardResponse.MlaRow(mla.getId(), name, dId, dName, constituencyName, budget,
					utilized, balance, pendingAmt, pendingCount));
		}

		// sort by MLA name
		rows = rows.stream()
				.sorted(Comparator.comparing(StateDashboardResponse.MlaRow::getMlaName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		// totals for filtered set
		double tBudget = rows.stream().mapToDouble(StateDashboardResponse.MlaRow::getBudget).sum();
		double tUtil = rows.stream().mapToDouble(StateDashboardResponse.MlaRow::getFundUtilized).sum();
		double tBal = rows.stream().mapToDouble(StateDashboardResponse.MlaRow::getBalance).sum();
		double tPend = rows.stream().mapToDouble(StateDashboardResponse.MlaRow::getPendingAmount).sum();
		long tPC = rows.stream().mapToLong(r -> Optional.ofNullable(r.getPendingCount()).orElse(0L)).sum();

		// pagination
		int from = Math.max(0, (page - 1) * size);
		int to = Math.min(rows.size(), from + size);
		List<StateDashboardResponse.MlaRow> pageRows = (from < to) ? rows.subList(from, to) : List.of();

		out.setMlaRows(pageRows);
		out.setFooter(new StateDashboardResponse.RowFooter(tBudget, tUtil, tBal, tPend, tPC));
		out.setTotalElements(rows.size());
		out.setTotalPages((int) Math.ceil(rows.size() / (double) size));

		// cards for MLA
		Map<String, Object> cards = new LinkedHashMap<>();
		cards.put("totalBudget", tBudget);
		cards.put("disbursedAmount", tUtil);
		cards.put("balanceAmount", tBal);
		cards.put("pendingAmount", tPend);
		cards.put("pendingCount", tPC);
		cards.put("totalMLAs", rows.size());
		out.setCards(cards);
	}

	// ---------- MLC ------------

	private void fillMlc(StateDashboardResponse out, Long fyId, Long districtId, Long mlcId, String search,
			boolean showPendingOnly, int page, int size) {

		// pull budgets by FY
		List<MLCBudget> budgets = (fyId == null) ? mlcBudgetRepo.findAllActive()
				: mlcBudgetRepo.findByFinancialYear_Id(fyId);

		// group by MLC
		Map<Long, List<MLCBudget>> byMlc = budgets.stream().filter(b -> b.getMlc() != null)
				.collect(Collectors.groupingBy(b -> b.getMlc().getId(), LinkedHashMap::new, Collectors.toList()));

		List<StateDashboardResponse.MlcRow> rows = new ArrayList<>();

		for (var e : byMlc.entrySet()) {
			MLC mlc = e.getValue().get(0).getMlc();

			// filter by exact MLC if provided
			if (mlcId != null && !Objects.equals(mlc.getId(), mlcId))
				continue;

			District nd = resolveNodalDistrict(mlc);
			Long ndId = (nd != null ? nd.getId() : null);
			String ndName = (nd != null ? nd.getDistrictName() : null);

			// district filter checks nodal district
			if (districtId != null && !Objects.equals(districtId, ndId))
				continue;

			String name = Optional.ofNullable(mlc.getMlcName()).orElse("");

			double budget = e.getValue().stream().mapToDouble(b -> NZ(b.getAllocatedLimit())).sum();
			double utilized = e.getValue().stream().mapToDouble(b -> NZ(b.getUtilizedLimit())).sum();
			double balance = budget - utilized;

			double pendingAmt = (fyId != null)
					? NZ(fundDemandRepo.sumByMlcAndFyInStatuses(mlc.getId(), fyId,
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE)))
					: NZ(fundDemandRepo.sumByMlcInStatuses(mlc.getId(),
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE)));

			long pendingCount = (fyId != null)
					? fundDemandRepo.countByMlcAndFyInStatuses(mlc.getId(), fyId,
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE))
					: fundDemandRepo.countByMlcInStatuses(mlc.getId(),
							List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE));

			if (search != null && !search.isBlank()) {
				String s = search.toLowerCase();
				boolean hit = name.toLowerCase().contains(s) || (ndName != null && ndName.toLowerCase().contains(s));
				if (!hit)
					continue;
			}
			if (showPendingOnly && pendingAmt <= 0)
				continue;

			rows.add(new StateDashboardResponse.MlcRow(mlc.getId(), name, ndId, ndName, budget, utilized, balance,
					pendingAmt, pendingCount));
		}

		// sort
		rows = rows.stream()
				.sorted(Comparator.comparing(StateDashboardResponse.MlcRow::getMlcName, String.CASE_INSENSITIVE_ORDER))
				.toList();

		double tBudget = rows.stream().mapToDouble(StateDashboardResponse.MlcRow::getBudget).sum();
		double tUtil = rows.stream().mapToDouble(StateDashboardResponse.MlcRow::getFundUtilized).sum();
		double tBal = rows.stream().mapToDouble(StateDashboardResponse.MlcRow::getBalance).sum();
		double tPend = rows.stream().mapToDouble(StateDashboardResponse.MlcRow::getPendingAmount).sum();
		long tPC = rows.stream().mapToLong(r -> Optional.ofNullable(r.getPendingCount()).orElse(0L)).sum();

		int from = Math.max(0, (page - 1) * size);
		int to = Math.min(rows.size(), from + size);
		List<StateDashboardResponse.MlcRow> pageRows = (from < to) ? rows.subList(from, to) : List.of();

		out.setMlcRows(pageRows);
		out.setFooter(new StateDashboardResponse.RowFooter(tBudget, tUtil, tBal, tPend, tPC));
		out.setTotalElements(rows.size());
		out.setTotalPages((int) Math.ceil(rows.size() / (double) size));

		Map<String, Object> cards = new LinkedHashMap<>();
		cards.put("totalBudget", tBudget);
		cards.put("disbursedAmount", tUtil);
		cards.put("balanceAmount", tBal);
		cards.put("pendingAmount", tPend);
		cards.put("pendingCount", tPC);
		out.setCards(cards);
	}

	// ---------- HADP ------------

	private void fillHadp(StateDashboardResponse out, Long fyId, Long districtId, String search,
			boolean showPendingOnly, int page, int size) {

		// If you maintain HADP budgets, aggregate from HADPBudget.
		// Otherwise, aggregate from Works (hadp != null) + Demands.
		var rows = new ArrayList<StateDashboardResponse.HadpRow>();

		// Strategy: aggregate per Taluka (frontend groups by demandCode, but table can
		// show taluka/demandCode)
		List<Taluka> talukas = (districtId == null) ? talukaRepo.findAll() : talukaRepo.findByDistrict_Id(districtId);

		for (Taluka t : talukas) {
			// budgets for this taluka (optional: limit by FY)
			double budget = NZ(hadpBudgetRepo.sumAllocatedByTalukaAndFyNullable(t.getId(), fyId));
			double utilized = NZ(hadpBudgetRepo.sumUtilizedByTalukaAndFyNullable(t.getId(), fyId));
			double balance = budget - utilized;

			// pending
			double pendingAmt = NZ(fundDemandRepo.sumByHadpTalukaAndFyInStatuses(t.getId(), fyId,
					List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE)));
			long pendingCount = fundDemandRepo.countByHadpTalukaAndFyInStatuses(t.getId(), fyId,
					List.of(DemandStatuses.PENDING, DemandStatuses.APPROVED_BY_STATE));

			String talukaName = t.getTalukaName();
			String districtName = t.getDistrict() != null ? t.getDistrict().getDistrictName() : null;
			Long dId = t.getDistrict() != null ? t.getDistrict().getId() : null;

			if (search != null && !search.isBlank()) {
				String s = search.toLowerCase();
				boolean hit = talukaName.toLowerCase().contains(s)
						|| (districtName != null && districtName.toLowerCase().contains(s));
				if (!hit)
					continue;
			}
			if (showPendingOnly && pendingAmt <= 0)
				continue;

			rows.add(new StateDashboardResponse.HadpRow(t.getId(), talukaName, dId, districtName,
					talukaName /* use as demandCode or compute real code if you have */, budget, utilized, balance,
					pendingAmt, pendingCount));
		}

		// already: ArrayList<StateDashboardResponse.HadpRow> rows = new ArrayList<>();
		rows.sort(Comparator.comparing(StateDashboardResponse.HadpRow::getDemandCode,
				Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

		double tBudget = rows.stream().mapToDouble(StateDashboardResponse.HadpRow::getBudget).sum();
		double tUtil = rows.stream().mapToDouble(StateDashboardResponse.HadpRow::getFundUtilized).sum();
		double tBal = rows.stream().mapToDouble(StateDashboardResponse.HadpRow::getBalance).sum();
		double tPend = rows.stream().mapToDouble(StateDashboardResponse.HadpRow::getPendingAmount).sum();
		long tPC = rows.stream().mapToLong(r -> Optional.ofNullable(r.getPendingCount()).orElse(0L)).sum();

		int from = Math.max(0, (page - 1) * size);
		int to = Math.min(rows.size(), from + size);
		List<StateDashboardResponse.HadpRow> pageRows = (from < to) ? rows.subList(from, to) : List.of();

		out.setHadpRows(pageRows);
		out.setFooter(new StateDashboardResponse.RowFooter(tBudget, tUtil, tBal, tPend, tPC));
		out.setTotalElements(rows.size());
		out.setTotalPages((int) Math.ceil(rows.size() / (double) size));

		Map<String, Object> cards = new LinkedHashMap<>();
		cards.put("totalBudget", tBudget);
		cards.put("approvedAmount", tUtil);
		cards.put("balanceAmount", tBal);
		cards.put("pendingAmount", tPend);
		cards.put("pendingCount", tPC);
		out.setCards(cards);
	}

	// -------- Click-through pending list ---------

	public List<FundDemandDetailDto> getPendingDemands(String username, String type, Long refId, Long fyId,
			Long districtId, String search, int page, int size) {

		User me = userRepo.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
		if (!isState(me)) {
			throw new RuntimeException("Forbidden");
		}

		PageRequest pr = PageRequest.of(Math.max(0, page - 1), Math.max(1, size), Sort.by("demandDate").descending());

		Page<FundDemand> pg;
		if ("MLA".equalsIgnoreCase(type)) {
			pg = fundDemandRepo.findPendingByMla(refId, fyId, districtId, search, pr);
		} else if ("MLC".equalsIgnoreCase(type)) {
			pg = fundDemandRepo.findPendingByMlc(refId, fyId, districtId, search, pr);
		} else if ("HADP".equalsIgnoreCase(type)) {
			pg = fundDemandRepo.findPendingByHadpTaluka(refId, fyId, districtId, search, pr);
		} else {
			throw new IllegalArgumentException("type must be MLA | MLC | HADP");
		}

		return pg.getContent().stream().map(fd -> {
			Work w = fd.getWork();
			Vendor v = fd.getVendor();
			FinancialYear fy = fd.getFinancialYear();
			return new FundDemandDetailDto(fd.getId(), fd.getDemandId(), fy != null ? fy.getId() : null,
					fy != null ? fy.getFinacialYear() : null, w != null ? w.getId() : null,
					w != null ? w.getWorkName() : null, w != null ? w.getWorkCode() : null,
					v != null ? v.getId() : null, v != null ? v.getName() : null, NZ(fd.getAmount()),
					NZ(fd.getNetPayable()), fd.getDemandDate(),
					fd.getStatus() != null ? fd.getStatus().name() : "UNKNOWN",
					(w != null ? NZ(w.getAdminApprovedAmount()) : 0.0),
					(w != null ? NZ(w.getWorkPortionAmount()) : 0.0));
		}).toList();
	}

	// --- utilities ---

	private District resolveNodalDistrict(MLC mlc) {
		if (mlc == null)
			return null;
		if (mlc.getDistrict() != null)
			return mlc.getDistrict();
		if (mlc.getUser() != null && mlc.getUser().getDistrict() != null)
			return mlc.getUser().getDistrict();
		try {
			if (mlc.getAccessibleDistricts() != null && !mlc.getAccessibleDistricts().isEmpty()) {
				return mlc.getAccessibleDistricts().iterator().next();
			}
		} catch (Exception ignored) {
		}
		return null;
	}
}
