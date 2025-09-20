// src/main/java/com/lsit/dfds/service/DistrictDashboardService.java
package com.lsit.dfds.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.DistrictDashboardFilterDto;
import com.lsit.dfds.dto.DistrictDashboardResponseDto;
import com.lsit.dfds.dto.DistrictDemandCountersDto;
import com.lsit.dfds.dto.DistrictOverviewDto;
import com.lsit.dfds.dto.FundDemandDetailDto;
import com.lsit.dfds.dto.MlaSummaryRowDto;
import com.lsit.dfds.dto.MlcSummaryRowDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistrictDashboardService {

	private final UserRepository userRepo;
	private final MLABudgetRepository mlaBudgetRepo;
	private final MLCBudgetRepository mlcBudgetRepo;
	private final MLCRepository mlcRepo;
	private final WorkRepository workRepo;
	private final FundDemandRepository demandRepo;

	private static double nz(Double d) {
		return d == null ? 0.0 : d;
	}

	private static long nz(Long l) {
		return l == null ? 0L : l;
	}

	private boolean isState(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private boolean isDistrict(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("DISTRICT");
	}

	@Transactional(readOnly = true)
	public DistrictDashboardResponseDto getDashboard(String username, DistrictDashboardFilterDto filter) {

		User me = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		Long fyId = filter.getFinancialYearId();
		PlanType plan = filter.getPlanType();

		// Resolve district
		Long districtId;
		if (isState(me)) {
			// State must pass district explicitly (to match UI “District: Pune”)
			districtId = me.getDistrict() != null ? me.getDistrict().getId() : null; // fallback if you want
			if (districtId == null) {
				throw new RuntimeException("State view requires districtId (set via user or pass param)");
			}
		} else if (isDistrict(me)) {
			if (me.getDistrict() == null) {
				throw new RuntimeException("User is not mapped to a district");
			}
			districtId = me.getDistrict().getId();
		} else {
			throw new RuntimeException("Only STATE_* and DISTRICT_* roles can view District dashboard");
		}

		String districtName = me.getDistrict() != null ? me.getDistrict().getDistrictName() : "N/A";

		// ---------- Overview Cards ----------
		Map<String, DistrictOverviewDto> overview = new LinkedHashMap<>();

		// MLA overview
		if (plan == null || plan == PlanType.MLA) {
			double totalBudget = nz(mlaBudgetRepo.sumAllocatedByDistrictAndFy(districtId, fyId));
			double disbursed = nz(demandRepo.sumTransferredByDistrict(districtId, fyId)); // across all plans; trim
																							// below
			// restrict disbursed to MLA only
			Double mlaDisbursed = nz(demandRepo.findDistrictDemands(districtId, fyId, null, null, null).stream()
					.filter(d -> d.getWork() != null && d.getWork().getRecommendedByMla() != null
							&& d.getStatus() == DemandStatuses.TRANSFERRED)
					.mapToDouble(FundDemand::getNetPayable).sum());
			long pending = nz(demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.PENDING));

			overview.put("MLA",
					new DistrictOverviewDto(totalBudget, mlaDisbursed, totalBudget - mlaDisbursed, pending));
		}

		// MLC overview (over MLCs that touch this district)
		if (plan == null || plan == PlanType.MLC) {
			// Which MLCs have works in this district (or accessible nodal)? Use works scan
			// for simplicity.
			List<Long> mlcIds = workRepo.findDistrictWorks(districtId, null, null, fyId).stream()
					.filter(w -> w.getRecommendedByMlc() != null).map(w -> w.getRecommendedByMlc().getId()).distinct()
					.toList();

			double totalBudget = mlcIds.isEmpty() ? 0.0 : nz(mlcBudgetRepo.sumAllocatedForMlcs(mlcIds, fyId));
			double disbursed = nz(demandRepo.findDistrictDemands(districtId, fyId, null, null, null).stream()
					.filter(d -> d.getWork() != null && d.getWork().getRecommendedByMlc() != null
							&& d.getStatus() == DemandStatuses.TRANSFERRED)
					.mapToDouble(FundDemand::getNetPayable).sum());
			long pending = nz(demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.PENDING));

			overview.put("MLC", new DistrictOverviewDto(totalBudget, disbursed, totalBudget - disbursed, pending));
		}

		// HADP overview – (if you store HADPBudget by district, add a repository and
		// sum it)
		// For now: provide a zero slot so FE card renders; wire your
		// HADPBudgetRepository similarly to MLA/MLC.
		if (plan == null || plan == PlanType.HADP) {
			overview.put("HADP", new DistrictOverviewDto(0.0, 0.0, 0.0, 0L));
		}

		// DAP overview – optional if you have DistrictLimitAllocation rows
		if (plan == null || plan == PlanType.DAP) {
			overview.put("DAP", new DistrictOverviewDto(0.0, 0.0, 0.0, 0L));
		}

		// ---------- “MLA Details — <District>” table ----------
		List<MlaSummaryRowDto> mlaRows = List.of();
		if (plan == null || plan == PlanType.MLA) {
			// all MLA budgets within the district (optionally FY)
			List<MLABudget> mlaBudgets = mlaBudgetRepo.findByDistrictAndFy(districtId, fyId);

			// group by MLA
			Map<Long, List<MLABudget>> byMla = mlaBudgets.stream()
					.collect(Collectors.groupingBy(b -> b.getMla().getId()));

			mlaRows = byMla.entrySet().stream().map(e -> {
				var any = e.getValue().get(0);
				Long mlaId = any.getMla().getId();
				String mlaName = any.getMla().getMlaName();
				String consName = any.getConstituency() != null ? any.getConstituency().getConstituencyName()
						: (any.getMla().getConstituency() != null ? any.getMla().getConstituency().getConstituencyName()
								: "—");

				double budget = e.getValue().stream().mapToDouble(b -> nz(b.getAllocatedLimit())).sum();
				double disb = nz(demandRepo.sumTransferredForMla(districtId, fyId, mlaId));
				long pendingMla = demandRepo.findDistrictDemands(districtId, fyId, mlaId, null, null).stream()
						.filter(d -> d.getStatus() == DemandStatuses.PENDING
								|| d.getStatus() == DemandStatuses.SENT_BACK_BY_STATE)
						.count();

				MlaSummaryRowDto row = new MlaSummaryRowDto();
				row.setMlaId(mlaId);
				row.setMlaName(mlaName);
				row.setConstituencyName(consName);
				row.setBudgetedAmount(budget);
				row.setFundsDisbursed(disb);
				row.setBalanceFunds(budget - disb);
				row.setPendingDemands(pendingMla);
				return row;
			}).sorted(Comparator.comparing(MlaSummaryRowDto::getMlaName, String.CASE_INSENSITIVE_ORDER)).toList();
		}

		// ---------- MLC summary table (optional) ----------
		List<MlcSummaryRowDto> mlcRows = List.of();
		if (plan == null || plan == PlanType.MLC) {
			// MLCs which have works in this district
			List<Work> wks = workRepo.findDistrictWorks(districtId, null, null, fyId).stream()
					.filter(w -> w.getRecommendedByMlc() != null).toList();
			Map<Long, List<Work>> byMlc = wks.stream()
					.collect(Collectors.groupingBy(w -> w.getRecommendedByMlc().getId()));
			mlcRows = byMlc.entrySet().stream().map(e -> {
				Long mlcId = e.getKey();
				MLC mlc = mlcRepo.findById(mlcId).orElse(null);

				double budget = nz(mlcBudgetRepo.sumAllocatedForMlcs(List.of(mlcId), fyId));
				double disb = nz(demandRepo.sumTransferredForMlc(districtId, fyId, mlcId));
				long pending = demandRepo.findDistrictDemands(districtId, fyId, null, mlcId, null).stream()
						.filter(d -> d.getStatus() == DemandStatuses.PENDING
								|| d.getStatus() == DemandStatuses.SENT_BACK_BY_STATE)
						.count();

				long nodal = e.getValue().stream().filter(w -> Boolean.TRUE.equals(w.getIsNodalWork())).count();
				long nonNodal = e.getValue().size() - nodal;

				MlcSummaryRowDto row = new MlcSummaryRowDto();
				row.setMlcId(mlcId);
				row.setMlcName(mlc != null ? mlc.getMlcName() : "—");
				row.setBudgetedAmount(budget);
				row.setFundsDisbursed(disb);
				row.setBalanceFunds(budget - disb);
				row.setPendingDemands(pending);
				row.setNodalWorks(nodal);
				row.setNonNodalWorks(nonNodal);
				return row;
			}).sorted(Comparator.comparing(MlcSummaryRowDto::getMlcName, String.CASE_INSENSITIVE_ORDER)).toList();
		}

		// ---------- IA Demands (cards block in second screenshot) ----------
		DistrictDemandCountersDto counters = new DistrictDemandCountersDto();
		// helpers
		Double amtApprovedByDist = nz(
				demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.APPROVED_BY_DISTRICT));
		Long cntApprovedByDist = nz(
				demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.APPROVED_BY_DISTRICT));

		Double amtApprovedByState = nz(
				demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.APPROVED_BY_STATE));
		Long cntApprovedByState = nz(
				demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.APPROVED_BY_STATE));

		Double amtRejectedByState = nz(
				demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.REJECTED_BY_STATE));
		Long cntRejectedByState = nz(
				demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.REJECTED_BY_STATE));

		Double amtPendingState = Math.max(0.0, amtApprovedByDist - amtApprovedByState - amtRejectedByState);
		Long cntPendingState = Math.max(0L, cntApprovedByDist - cntApprovedByState - cntRejectedByState);

		// Received from IA = PENDING + SENT_BACK_BY_STATE + (optionally
		// REJECTED/APPROVED_BY_DISTRICT as total received)
		Double amtPendingDist = nz(demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.PENDING))
				+ nz(demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.SENT_BACK_BY_STATE));
		Long cntPendingDist = nz(demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.PENDING))
				+ nz(demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.SENT_BACK_BY_STATE));

		Double amtRejectedByDist = nz(
				demandRepo.sumByDistrictAndStatus(districtId, fyId, DemandStatuses.REJECTED_BY_DISTRICT));
		Long cntRejectedByDist = nz(
				demandRepo.countByDistrictAndStatus(districtId, fyId, DemandStatuses.REJECTED_BY_DISTRICT));

		// “receivedFromIa” total = pending + approved + rejected at district
		Double amtReceivedFromIa = amtPendingDist + amtApprovedByDist + amtRejectedByDist;
		Long cntReceivedFromIa = cntPendingDist + cntApprovedByDist + cntRejectedByDist;

		DistrictDemandCountersDto.Block blk = new DistrictDemandCountersDto.Block();
		// Submitted to State
		DistrictDemandCountersDto.Block submittedTotal = new DistrictDemandCountersDto.Block();
		submittedTotal.setAmount(amtApprovedByDist);
		submittedTotal.setCount(cntApprovedByDist);

		DistrictDemandCountersDto.Block approvedByStateBlk = new DistrictDemandCountersDto.Block();
		approvedByStateBlk.setAmount(amtApprovedByState);
		approvedByStateBlk.setCount(cntApprovedByState);

		DistrictDemandCountersDto.Block rejectedByStateBlk = new DistrictDemandCountersDto.Block();
		rejectedByStateBlk.setAmount(amtRejectedByState);
		rejectedByStateBlk.setCount(cntRejectedByState);

		DistrictDemandCountersDto.Block pendingStateBlk = new DistrictDemandCountersDto.Block();
		pendingStateBlk.setAmount(amtPendingState);
		pendingStateBlk.setCount(cntPendingState);

		counters.setSubmittedTotal(submittedTotal);
		counters.setApprovedByState(approvedByStateBlk);
		counters.setRejectedByState(rejectedByStateBlk);
		counters.setPendingAtState(pendingStateBlk);

		// Received from IA
		DistrictDemandCountersDto.Block receivedBlk = new DistrictDemandCountersDto.Block();
		receivedBlk.setAmount(amtReceivedFromIa);
		receivedBlk.setCount(cntReceivedFromIa);

		DistrictDemandCountersDto.Block approvedDistBlk = new DistrictDemandCountersDto.Block();
		approvedDistBlk.setAmount(amtApprovedByDist);
		approvedDistBlk.setCount(cntApprovedByDist);

		DistrictDemandCountersDto.Block rejectedDistBlk = new DistrictDemandCountersDto.Block();
		rejectedDistBlk.setAmount(amtRejectedByDist);
		rejectedDistBlk.setCount(cntRejectedByDist);

		DistrictDemandCountersDto.Block pendingDistBlk = new DistrictDemandCountersDto.Block();
		pendingDistBlk.setAmount(amtPendingDist);
		pendingDistBlk.setCount(cntPendingDist);

		counters.setReceivedFromIa(receivedBlk);
		counters.setApprovedByDistrict(approvedDistBlk);
		counters.setRejectedByDistrict(rejectedDistBlk);
		counters.setPendingAtDistrict(pendingDistBlk);

		// ---------- Demands table (send ids like Demand ID, Work Code, Vendor etc.)
		// ----------
		List<FundDemand> districtDemands = demandRepo.findDistrictDemands(districtId, fyId, filter.getMlaId(),
				filter.getMlcId(), null);

		List<FundDemandDetailDto> demandRows = districtDemands.stream().map(fd -> {
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

		// ---------- Assemble ----------
		DistrictDashboardResponseDto out = new DistrictDashboardResponseDto();
		out.setFinancialYearId(fyId);
		out.setPlanType(plan);
		out.setDistrictId(districtId);
		out.setDistrictName(districtName);

		out.setOverview(overview);
		out.setMlaRows(mlaRows);
		out.setMlcRows(mlcRows);
		out.setDemandsSummary(counters);
		out.setDemands(demandRows);

		return out;
	}
}
