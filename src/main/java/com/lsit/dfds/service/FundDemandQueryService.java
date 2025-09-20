// src/main/java/com/lsit/dfds/service/FundDemandQueryService.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.FundDemandListItemDto;
import com.lsit.dfds.dto.FundDemandQueryDto;
import com.lsit.dfds.dto.TaxDetailDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundDemandQueryService {

	private final FundDemandRepository fundDemandRepository;
	private final UserRepository userRepository;

	public List<FundDemandListItemDto> listForUser(String username, FundDemandQueryDto q) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long financialYearId = q.getFinancialYearId();
		String search = (q.getQ() == null || q.getQ().isBlank()) ? null : q.getQ().trim();
		var status = q.getStatus();

		Long districtId = null;
		String createdBy = null;

		if (me.getRole().name().startsWith("STATE")) {
			districtId = q.getDistrictId();
		} else if (me.getRole().name().startsWith("DISTRICT")) {
			if (me.getDistrict() == null) {
				throw new RuntimeException("User is not mapped to a district");
			}
			districtId = me.getDistrict().getId();
		} else if (me.getRole() == Roles.IA_ADMIN) {
			createdBy = username;
		} else {
			throw new RuntimeException("Role not authorized for demands");
		}

		List<FundDemand> rows = fundDemandRepository.findFiltered(financialYearId, districtId, createdBy, status,
				search);

		return rows.stream().map(d -> {
			var w = d.getWork();
			var fy = d.getFinancialYear();

			String financialYear = (fy != null) ? fy.getFinacialYear() : null;
			String planType = (w != null && w.getScheme() != null && w.getScheme().getBaseSchemeType() != null)
					? w.getScheme().getBaseSchemeType().name()
					: null;
			String districtName = (w != null && w.getDistrict() != null) ? w.getDistrict().getDistrictName() : null;
			String constituencyName = (w != null && w.getConstituency() != null)
					? w.getConstituency().getConstituencyName()
					: null;

			Double workAaAmount = (w != null && w.getAdminApprovedAmount() != null) ? w.getAdminApprovedAmount() : 0.0;
			Double workGrossOrderAmount = (w != null && w.getGrossAmount() != null) ? w.getGrossAmount() : 0.0;

			String vendorName = (d.getVendor() != null) ? d.getVendor().getName() : null;

			Double demandGrossAmount = (d.getAmount() != null) ? d.getAmount() : 0.0;

			double demandTaxTotal = 0.0;
			List<TaxDetailDto> demandTaxDetails = null;
			if (d.getTaxes() != null && !d.getTaxes().isEmpty()) {
				demandTaxTotal = d.getTaxes().stream().map(t -> t.getTaxAmount() == null ? 0.0 : t.getTaxAmount())
						.mapToDouble(Double::doubleValue).sum();

				demandTaxDetails = d.getTaxes().stream().map(t -> {
					TaxDetailDto x = new TaxDetailDto();
					x.setTaxName(t.getTaxName());
					x.setTaxPercentage(t.getTaxPercentage());
					x.setTaxAmount(t.getTaxAmount());
					return x;
				}).toList();
			}

			Double demandNetAmount = (d.getNetPayable() != null) ? d.getNetPayable()
					: (demandGrossAmount - demandTaxTotal);

			Long mlaId = null;
			String mlaName = null;
			if (w != null && w.getRecommendedByMla() != null) {
				mlaId = w.getRecommendedByMla().getId();
				mlaName = w.getRecommendedByMla().getMlaName(); // adjust if getter differs
			}

			return FundDemandListItemDto.builder().fundDemandId(d.getId()).demandId(d.getDemandId())
					.financialYear(financialYear).planType(planType).districtName(districtName)
					.constituencyName(constituencyName)

					.workName(w != null ? w.getWorkName() : null).workCode(w != null ? w.getWorkCode() : null)
					.workAaAmount(workAaAmount).workGrossOrderAmount(workGrossOrderAmount)

					.vendorName(vendorName)

					.demandGrossAmount(demandGrossAmount).demandTaxTotal(demandTaxTotal)
					.demandTaxDetails(demandTaxDetails).demandNetAmount(demandNetAmount)

					.status(d.getStatus() != null ? d.getStatus().name() : null).demandDate(d.getDemandDate())
					.createdAt(d.getCreatedAt()).createdBy(d.getCreatedBy())

					.mlaId(mlaId).mlaName(mlaName).build();
		}).toList();
	}
}
