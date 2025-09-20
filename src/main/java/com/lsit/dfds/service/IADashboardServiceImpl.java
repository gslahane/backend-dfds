package com.lsit.dfds.service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.entity.TaxDeductionFundTransfer;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.FundTransferRepository;
import com.lsit.dfds.repo.FundTransferVoucherRepository;
import com.lsit.dfds.repo.TaxDeductionFundTransferRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IADashboardServiceImpl implements IADashboardService {

	private final FundDemandRepository fundDemandRepo;
	private final FundTransferRepository fundTransferRepo;
	private final TaxDeductionFundTransferRepository taxTransferRepo;
	private final FundTransferVoucherRepository voucherRepo;
	private final WorkRepository workRepo;
	private final UserRepository userRepo;

	@Override
	public Map<String, Object> getDashboardData(String username, String financialYear, // e.g. "2025-2026"
			String planType, // "DAP" | "MLA" | "MLC" | "HADP" | null
			Long schemeId, Long workId, String status) {
		User iaUser = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// Fetch all IA works (then filter in-memory)
		List<Work> iaWorks = workRepo.findAll().stream()
				.filter(w -> w != null && w.getImplementingAgency() != null
						&& Objects.equals(w.getImplementingAgency().getId(), iaUser.getId()))
				.collect(Collectors.toList());

		// Apply optional work-level filters
		List<Work> filteredWorks = iaWorks.stream().filter(w -> filterByFY(w, financialYear))
				.filter(w -> filterByPlanType(w, planType))
				.filter(w -> schemeId == null
						|| (w.getScheme() != null && Objects.equals(w.getScheme().getId(), schemeId)))
				.filter(w -> workId == null || Objects.equals(w.getId(), workId)).collect(Collectors.toList());

		// Grab IA demands (created by IA or for IA-owned works)
		List<FundDemand> allIADemands = fundDemandRepo.findAll().stream()
				.filter(d -> d != null && !Boolean.TRUE.equals(d.getIsDelete()))
				.filter(d -> Objects.equals(username, d.getCreatedBy())
						|| (d.getWork() != null && d.getWork().getImplementingAgency() != null
								&& Objects.equals(d.getWork().getImplementingAgency().getId(), iaUser.getId())))
				.collect(Collectors.toList());

		// Filter demands by provided filters (status guard is null-safe)
		List<FundDemand> demands = allIADemands.stream().filter(d -> filterByFY(d, financialYear))
				.filter(d -> filterByPlanType(d, planType))
				.filter(d -> schemeId == null || (d.getWork() != null && d.getWork().getScheme() != null
						&& Objects.equals(d.getWork().getScheme().getId(), schemeId)))
				.filter(d -> workId == null || (d.getWork() != null && Objects.equals(d.getWork().getId(), workId)))
				.filter(d -> {
					if (status == null || status.isBlank()) {
						return true;
					}
					return d.getStatus() != null && d.getStatus().name().equalsIgnoreCase(status);
				}).collect(Collectors.toList());

		// ===== Cards / KPIs =====

		// Demanded to State = sum of gross demand amounts
		double demandedToState = demands.stream().mapToDouble(d -> nz(d.getAmount())).sum();

		// Received from State = vendor payments + tax transfers
		Set<Long> demandIds = demands.stream().map(FundDemand::getId).collect(Collectors.toSet());

		List<FundTransfer> transfers = fundTransferRepo.findAll().stream().filter(
				ft -> ft != null && ft.getFundDemand() != null && demandIds.contains(ft.getFundDemand().getId()))
				.collect(Collectors.toList());
		double vendorPaid = transfers.stream().mapToDouble(ft -> nz(ft.getTransferAmount())).sum();

		List<TaxDeductionFundTransfer> taxTransfers = taxTransferRepo.findAll().stream().filter(
				tt -> tt != null && tt.getFundDemand() != null && demandIds.contains(tt.getFundDemand().getId()))
				.filter(tt -> {
					String st = tt.getStatus();
					return st != null && st.equalsIgnoreCase("Transferred");
				}).collect(Collectors.toList());
		double taxPaid = taxTransfers.stream().mapToDouble(tt -> nz(tt.getTotalTaxAmount())).sum();

		double receivedFromState = vendorPaid + taxPaid;

		// AA (District Approved): count of works with AA_APPROVED or later + total
		// adminApprovedAmount
		Set<WorkStatuses> aaOrLater = EnumSet.of(WorkStatuses.AA_APPROVED, WorkStatuses.WORK_ORDER_ISSUED,
				WorkStatuses.IN_PROGRESS, WorkStatuses.COMPLETED);

		long aaCount = filteredWorks.stream().filter(w -> w.getStatus() != null && aaOrLater.contains(w.getStatus()))
				.count();

		double aaAmount = filteredWorks.stream().filter(w -> w.getStatus() != null && aaOrLater.contains(w.getStatus()))
				.mapToDouble(w -> nz(w.getAdminApprovedAmount())).sum();

		// Demands breakdown
		long rejectedDemands = demands.stream().filter(d -> d.getStatus() == DemandStatuses.REJECTED_BY_DISTRICT
				|| d.getStatus() == DemandStatuses.REJECTED_BY_STATE).count();

		long approvedDemands = demands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.APPROVED_BY_DISTRICT
						|| d.getStatus() == DemandStatuses.APPROVED_BY_STATE
						|| d.getStatus() == DemandStatuses.TRANSFERRED)
				.count();

		long pendingDemands = demands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.PENDING
						|| d.getStatus() == DemandStatuses.SENT_BACK_BY_DISTRICT
						|| d.getStatus() == DemandStatuses.SENT_BACK_BY_STATE)
				.count();

		// Vendor mapping
		long mappingDoneCount = filteredWorks.stream().filter(w -> w.getVendor() != null).count();

		double mappingDoneAmount = filteredWorks.stream().filter(w -> w.getVendor() != null)
				.mapToDouble(w -> nz(w.getAdminApprovedAmount())).sum();

		long mappingPendingCount = filteredWorks.stream().filter(w -> w.getVendor() == null).count();

		double mappingPendingAmount = filteredWorks.stream().filter(w -> w.getVendor() == null)
				.mapToDouble(w -> nz(w.getAdminApprovedAmount())).sum();

		// ===== Demands table =====
		List<Map<String, Object>> demandRows = demands.stream().sorted(Comparator
				.comparing(FundDemand::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
				.map(d -> {
					Work w = d.getWork();
					Vendor v = d.getVendor();

					FundTransfer ft = transfers.stream().filter(
							t -> t.getFundDemand() != null && Objects.equals(t.getFundDemand().getId(), d.getId()))
							.findFirst().orElse(null);

					FundTransferVoucher voucher = voucherRepo.findByFundDemand_Id(d.getId()).orElse(null);

					double taxTotal = (d.getTaxes() == null) ? 0.0
							: d.getTaxes().stream().mapToDouble(t -> nz(t.getTaxAmount())).sum();

					Map<String, Object> row = new LinkedHashMap<>();
					row.put("demandId", d.getDemandId());
					row.put("fundDemandId", d.getId());
					row.put("demandDate", d.getDemandDate());
					row.put("status", d.getStatus() != null ? d.getStatus().name() : null);

					if (w != null) {
						row.put("workId", w.getId());
						row.put("workCode", w.getWorkCode());
						row.put("workTitle", w.getWorkName());
						row.put("schemeId", (w.getScheme() != null) ? w.getScheme().getId() : null);
						row.put("schemeName", (w.getScheme() != null) ? w.getScheme().getSchemeName() : null);
						row.put("planType",
								(w.getScheme() != null && w.getScheme().getPlanType() != null)
										? w.getScheme().getPlanType().name()
										: null);
						row.put("aaAmount", nz(w.getAdminApprovedAmount()));
					}

					if (v != null) {
						row.put("vendorId", v.getId());
						row.put("vendorName", v.getName());
					}

					row.put("grossAmount", nz(d.getAmount()));
					row.put("taxTotal", taxTotal);
					row.put("netPayable", nz(d.getNetPayable()));

					if (ft != null) {
						row.put("transferred", true);
						row.put("transferAmount", nz(ft.getTransferAmount()));
						row.put("paymentRefNo", ft.getPaymentRefNo());
						row.put("transferDate", ft.getCreatedAt());
					} else {
						row.put("transferred", false);
					}

					row.put("voucherNo", (voucher != null) ? voucher.getVoucherNo() : null);

					return row;
				}).collect(Collectors.toList());

		// ===== Response =====
		Map<String, Object> response = new LinkedHashMap<>();

		// Null-safe filtersEcho (LinkedHashMap allows null values)
		Map<String, Object> filters = new LinkedHashMap<>();
		// If you prefer to OMIT nulls, wrap each put with a null check instead.
		filters.put("financialYear", financialYear);
		filters.put("planType", planType);
		filters.put("schemeId", schemeId);
		filters.put("workId", workId);
		filters.put("status", status);
		response.put("filtersEcho", filters);

		// All numeric values are computed (non-null), so Map.of is fine in the rest
		response.put("state", Map.of("demandedToState", demandedToState, "receivedFromState", receivedFromState));

		response.put("aa", Map.of("aaApprovedWorksCount", aaCount, "aaApprovedAmount", aaAmount));

		response.put("demandsSummary", Map.of("totalDemands", demands.size(), "approvedDemands", approvedDemands,
				"pendingDemands", pendingDemands, "rejectedDemands", rejectedDemands));

		response.put("vendorMapping",
				Map.of("mappingDoneCount", mappingDoneCount, "mappingDoneAmount", mappingDoneAmount,
						"mappingPendingCount", mappingPendingCount, "mappingPendingAmount", mappingPendingAmount));

		response.put("demands", demandRows);
		return response;
	}

	// ===== Helpers =====
	private static double nz(Double d) {
		return (d == null) ? 0.0 : d;
	}

	// ---------- Financial Year label helpers ----------
	private static String normalizeFy(String s) {
		if (s == null) {
			return null;
		}
		String t = s.trim().replace('–', '-').replace('—', '-'); // normalize dashes
		if (t.matches("^\\d{4}-\\d{2}$")) { // e.g. 2025-26 -> 2025-2026
			int start = Integer.parseInt(t.substring(0, 4));
			int end2 = Integer.parseInt(t.substring(5, 7));
			int end = (start / 100) * 100 + end2;
			return start + "-" + end;
		}
		return t;
	}

	// ================== WORK ==================

	// Match by FY *label* (e.g., "2025-2026" or "2025-26")
	private static boolean filterByFY(Work w, String fyLabel) {
		if (fyLabel == null || fyLabel.trim().isEmpty()) {
			return true;
		}
		String target = normalizeFy(fyLabel);

		String workFy = (w != null && w.getFinancialYear() != null) ? w.getFinancialYear().getFinacialYear() : null;

		String schemeFy = (w != null && w.getScheme() != null && w.getScheme().getFinancialYear() != null)
				? w.getScheme().getFinancialYear().getFinacialYear()
				: null;

		return (workFy != null && normalizeFy(workFy).equalsIgnoreCase(target))
				|| (schemeFy != null && normalizeFy(schemeFy).equalsIgnoreCase(target));
	}

	// Match by FY *ID*
	private static boolean filterByFY(Work w, Long fyId) {
		if (fyId == null) {
			return true;
		}

		Long workFyId = (w != null && w.getFinancialYear() != null) ? w.getFinancialYear().getId() : null;

		Long schemeFyId = (w != null && w.getScheme() != null && w.getScheme().getFinancialYear() != null)
				? w.getScheme().getFinancialYear().getId()
				: null;

		return Objects.equals(fyId, workFyId) || Objects.equals(fyId, schemeFyId);
	}

	// ================== FUND DEMAND ==================

	// Match by FY *label*
	private static boolean filterByFY(FundDemand d, String fyLabel) {
		if (fyLabel == null || fyLabel.trim().isEmpty()) {
			return true;
		}
		String target = normalizeFy(fyLabel);

		String demandFy = (d != null && d.getFinancialYear() != null) ? d.getFinancialYear().getFinacialYear() : null;

		return demandFy != null && normalizeFy(demandFy).equalsIgnoreCase(target);
	}

	// Match by FY *ID*
	private static boolean filterByFY(FundDemand d, Long fyId) {
		if (fyId == null) {
			return true;
		}

		Long demandFyId = (d != null && d.getFinancialYear() != null) ? d.getFinancialYear().getId() : null;

		return Objects.equals(fyId, demandFyId);
	}

	private static boolean filterByPlanType(Work w, String planType) {
		if (planType == null || planType.isBlank()) {
			return true;
		}
		PlanType p = (w.getScheme() != null) ? w.getScheme().getPlanType() : null;
		return p != null && p.name().equalsIgnoreCase(planType);
	}

	private static boolean filterByPlanType(FundDemand d, String planType) {
		if (planType == null || planType.isBlank()) {
			return true;
		}
		Work w = d.getWork();
		if (w == null || w.getScheme() == null) {
			return false;
		}
		return w.getScheme().getPlanType() != null && w.getScheme().getPlanType().name().equalsIgnoreCase(planType);
	}
}
