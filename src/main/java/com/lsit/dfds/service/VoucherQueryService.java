// src/main/java/com/lsit/dfds/service/VoucherQueryService.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.VoucherListItemDto;
import com.lsit.dfds.dto.VoucherQueryDto;
import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.FundTransferVoucherRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VoucherQueryService {

	private final FundTransferVoucherRepository voucherRepo;
	private final UserRepository userRepository;

	private static double NZ(Double d) {
		return d == null ? 0.0 : d;
	}

	public List<VoucherListItemDto> listForUser(String username, VoucherQueryDto q) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long financialYearId = q.getFinancialYearId();
		String search = (q.getQ() == null || q.getQ().isBlank()) ? null : q.getQ().trim();
		var status = q.getStatus();

		Long districtId = null;
		String createdBy = null;

		if (me.getRole().name().startsWith("STATE")) {
			// State: can filter by any district or FY; no createdBy restriction
			districtId = q.getDistrictId();
		} else if (me.getRole().name().startsWith("DISTRICT")) {
			// District: enforce own district
			if (me.getDistrict() == null) {
				throw new RuntimeException("User is not mapped to a district");
			}
			districtId = me.getDistrict().getId();
		} else if (me.getRole() == Roles.IA_ADMIN) {
			// IA: only own created
			createdBy = username;
		} else {
			// others: default deny or restrict further if needed
			throw new RuntimeException("Role not authorized for vouchers");
		}

		List<FundTransferVoucher> rows = voucherRepo.findFiltered(financialYearId, districtId, createdBy, status,
				search);

		return rows.stream().map(v -> {
			var d = v.getFundDemand();
			var w = d.getWork();
			var fy = d.getFinancialYear();
			var dist = (w != null ? w.getDistrict() : null);
			return new VoucherListItemDto(d.getId(), v.getVoucherNo(), d.getDemandId(),
					fy != null ? fy.getFinacialYear() : null, dist != null ? dist.getDistrictName() : null,
					w != null ? w.getWorkName() : null, w != null ? w.getWorkCode() : null, v.getVendorName(),
					v.getGrossAmount(), v.getTaxTotal(), v.getNetPayable(),
					d.getStatus() != null ? d.getStatus().name() : "UNKNOWN", v.getCreatedAt());
		}).toList();
	}
}
