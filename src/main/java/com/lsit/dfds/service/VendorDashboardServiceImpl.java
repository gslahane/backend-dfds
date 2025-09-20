// src/main/java/com/lsit/dfds/service/VendorDashboardServiceImpl.java
package com.lsit.dfds.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.VendorDashboardResponseDto;
import com.lsit.dfds.dto.VendorDemandRowDto;
import com.lsit.dfds.dto.VendorWorkRowDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.FundTransferRepository;
import com.lsit.dfds.repo.FundTransferVoucherRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorDashboardServiceImpl implements VendorDashboardService {

	private final UserRepository userRepo;
	private final VendorRepository vendorRepo;
	private final FundDemandRepository fundDemandRepo;
	private final FundTransferRepository fundTransferRepo;
	private final FundTransferVoucherRepository voucherRepo;
	private final WorkRepository workRepo;

	@Override
	public VendorDashboardResponseDto getDashboard(String username, Long vendorId, Long financialYearId,
			Long districtId, Long schemeId, String statusStr, String search) {
		// 1) Resolve caller and vendor context
		User me = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long resolvedVendorId;
		if (me.getRole() == Roles.VENDOR) {
			Vendor myVendor = vendorRepo.findByUser_Id(me.getId())
					.orElseThrow(() -> new RuntimeException("Vendor profile not linked to user"));
			resolvedVendorId = myVendor.getId();
		} else {
			if (vendorId == null) {
				throw new RuntimeException("vendorId is required for non-vendor roles");
			}
			resolvedVendorId = vendorId;
		}

		DemandStatuses status = null;
		if (statusStr != null && !statusStr.isBlank()) {
			try {
				status = DemandStatuses.valueOf(statusStr.trim());
			} catch (IllegalArgumentException ex) {
				throw new RuntimeException("Invalid demand status: " + statusStr);
			}
		}

		String normSearch = (search == null || search.isBlank()) ? null : search.trim();

		// 2) Fetch demands
		List<FundDemand> demands = fundDemandRepo.findDemandsForVendor(resolvedVendorId, financialYearId, districtId,
				schemeId, status, normSearch);

		// 3) Fetch transfers for "received from State"
		List<FundTransfer> transfers = fundTransferRepo.findTransfersForVendor(resolvedVendorId, financialYearId);

		// 4) Build demand rows + compute counters
		long totalDemands = demands.size();
		long pendingDemands = demands.stream().filter(d -> isPending(d.getStatus())).count();
		long rejectedDemands = demands.stream().filter(d -> isRejected(d.getStatus())).count();
		long approvedByState = demands.stream().filter(d -> d.getStatus() == DemandStatuses.APPROVED_BY_STATE).count();
		long transferredDemands = demands.stream().filter(d -> d.getStatus() == DemandStatuses.TRANSFERRED).count();

		double totalGross = demands.stream().mapToDouble(d -> nz(d.getAmount())).sum();
		double totalNet = demands.stream().mapToDouble(d -> nz(d.getNetPayable())).sum();
		double totalTransferred = transfers.stream().mapToDouble(t -> nz(t.getTransferAmount())).sum();

		var demandRows = demands.stream().map(d -> {
			Work w = d.getWork();
			var maybeFt = fundTransferRepo.findByFundDemand_Id(d.getId());
			var maybeVch = voucherRepo.findByFundDemand_Id(d.getId());

			return new VendorDemandRowDto(d.getId(), d.getDemandId(),
					d.getFinancialYear() != null ? d.getFinancialYear().getId() : null,
					d.getFinancialYear() != null ? d.getFinancialYear().getFinacialYear() : null,
					w != null ? w.getId() : null, w != null ? w.getWorkCode() : null,
					w != null ? w.getWorkName() : null,
					w != null && w.getScheme() != null ? w.getScheme().getId() : null,
					w != null && w.getScheme() != null ? w.getScheme().getSchemeName() : null,
					w != null && w.getDistrict() != null ? w.getDistrict().getId() : null,
					w != null && w.getDistrict() != null ? w.getDistrict().getDistrictName() : null, d.getDemandDate(),
					nz(d.getAmount()), nz(d.getNetPayable()), d.getStatus() != null ? d.getStatus().name() : "UNKNOWN",
					maybeFt.map(FundTransfer::getPaymentRefNo).orElse(null),
					maybeVch.map(FundTransferVoucher::getVoucherNo).orElse(null));
		}).toList();

		// 5) Works assigned to this vendor (+ IA info)
		List<Work> works = workRepo.findWorksForVendor(resolvedVendorId, districtId, schemeId, normSearch);

		long totalWorks = works.size();
		Map<String, Long> worksByStatus = works.stream().collect(java.util.stream.Collectors.groupingBy(
				w -> w.getStatus() == null ? "UNKNOWN" : w.getStatus().name(), java.util.stream.Collectors.counting()));

		var workRows = works.stream()
				.map(w -> new VendorWorkRowDto(w.getId(), w.getWorkCode(), w.getWorkName(),
						w.getStatus() != null ? w.getStatus().name() : "UNKNOWN", nz(w.getAdminApprovedAmount()),
						nz(w.getBalanceAmount()), w.getScheme() != null ? w.getScheme().getId() : null,
						w.getScheme() != null ? w.getScheme().getSchemeName() : null,
						w.getDistrict() != null ? w.getDistrict().getId() : null,
						w.getDistrict() != null ? w.getDistrict().getDistrictName() : null,
						w.getImplementingAgency() != null ? w.getImplementingAgency().getId() : null,
						w.getImplementingAgency() != null ? w.getImplementingAgency().getUsername() : null,
						w.getImplementingAgency() != null ? nzName(w.getImplementingAgency().getFullname()) : null))
				.toList();

		// 6) Build response
		VendorDashboardResponseDto out = new VendorDashboardResponseDto();
		out.setTotalDemands(totalDemands);
		out.setPendingDemands(pendingDemands);
		out.setRejectedDemands(rejectedDemands);
		out.setApprovedByStateDemands(approvedByState);
		out.setTransferredDemands(transferredDemands);

		out.setTotalGrossDemanded(totalGross);
		out.setTotalNetDemanded(totalNet);
		out.setTotalTransferred(totalTransferred);

		out.setTotalWorks(totalWorks);
		out.setWorksByStatus(worksByStatus);

		out.setDemands(demandRows);
		out.setWorks(workRows);
		return out;
	}

	private static boolean isPending(DemandStatuses s) {
		return s == DemandStatuses.PENDING || s == DemandStatuses.APPROVED_BY_DISTRICT
				|| s == DemandStatuses.SENT_BACK_BY_DISTRICT || s == DemandStatuses.SENT_BACK_BY_STATE;
	}

	private static boolean isRejected(DemandStatuses s) {
		return s == DemandStatuses.REJECTED_BY_DISTRICT || s == DemandStatuses.REJECTED_BY_STATE;
	}

	private static double nz(Double d) {
		return d == null ? 0.0 : d;
	}

	private static String nzName(String s) {
		return (s == null || s.isBlank()) ? null : s;
	}
}
