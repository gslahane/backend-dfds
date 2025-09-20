package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.FundDemandFilterRequest;
import com.lsit.dfds.dto.FundDemandResponseDto;
import com.lsit.dfds.dto.TaxDetailDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundDemandFetchServiceImpl implements FundDemandFetchService {

	private final FundDemandRepository fundDemandRepository;
	private final UserRepository userRepository;
	private final JwtUtil jwtUtil;

	@Override
	public List<FundDemandResponseDto> getFilteredFundDemands(FundDemandFilterRequest filters,
			HttpServletRequest request) {
		String username = jwtUtil.extractUsernameFromRequest(request);
		if (username == null) {
			throw new RuntimeException("Unauthorized access");
		}

		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Roles role = user.getRole();

		// ✅ Fetch only active demands
		List<FundDemand> allDemands = fundDemandRepository.findByIsDeleteFalse();

		return allDemands.stream()
				// 🔹 Role-based filter
				.filter(fd -> {
					if (role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER) {
						return true; // State can see all
					} else if (role == Roles.DISTRICT_ADMIN || role == Roles.DISTRICT_CHECKER
							|| role == Roles.DISTRICT_MAKER) {
						return fd.getWork().getScheme().getDistricts().stream()
								.anyMatch(d -> d.getId().equals(user.getSupervisor().getId()));
					} else if (role == Roles.IA_ADMIN) {
						return username.equals(fd.getCreatedBy());
					}
					return false;
				})
				// 🔹 Filters
				.filter(fd -> filters.getFinancialYearId() == null
						|| fd.getFinancialYear().getId().equals(filters.getFinancialYearId()))
				.filter(fd -> filters.getSchemeTypeId() == null
						|| fd.getWork().getScheme().getSchemeType().getId().equals(filters.getSchemeTypeId()))
				.filter(fd -> filters.getSchemeId() == null
						|| fd.getWork().getScheme().getId().equals(filters.getSchemeId()))
				.filter(fd -> filters.getWorkId() == null || fd.getWork().getId().equals(filters.getWorkId()))
				.filter(fd -> filters.getMlaId() == null || (fd.getWork().getRecommendedByMla() != null
						&& fd.getWork().getRecommendedByMla().getId().equals(filters.getMlaId())))
				.filter(fd -> filters.getMlcId() == null || (fd.getWork().getRecommendedByMlc() != null
						&& fd.getWork().getRecommendedByMlc().getId().equals(filters.getMlcId())))
				.filter(fd -> filters.getDemandStatus() == null
						|| fd.getStatus().name().equals(filters.getDemandStatus()))
				.map(this::mapToDto).collect(Collectors.toList());
	}

	private FundDemandResponseDto mapToDto(FundDemand fd) {
		FundDemandResponseDto dto = new FundDemandResponseDto();
		dto.setDemandId(fd.getDemandId());
		dto.setSchemeName(fd.getWork().getScheme().getSchemeName());
		dto.setSchemeType(fd.getWork().getScheme().getSchemeType().getSchemeType());
		dto.setFinancialYear(fd.getFinancialYear().getFinacialYear());
		dto.setWorkName(fd.getWork().getWorkName());
		dto.setWorkCode(fd.getWork().getWorkCode());
		dto.setVendorName(fd.getVendor().getName());
		dto.setIaName(fd.getCreatedBy());
		dto.setAmount(fd.getAmount());
		dto.setNetPayable(fd.getNetPayable());
		dto.setDemandStatus(fd.getStatus().name());
		dto.setDemandDate(fd.getDemandDate());
		dto.setRemarks(fd.getRemarks());
		dto.setMlaName(
				fd.getWork().getRecommendedByMla() != null ? fd.getWork().getRecommendedByMla().getMlaName() : null);
		dto.setMlcName(
				fd.getWork().getRecommendedByMlc() != null ? fd.getWork().getRecommendedByMlc().getMlcName() : null);

		dto.setTaxes(fd.getTaxes().stream().map(t -> {
			TaxDetailDto taxDto = new TaxDetailDto();
			taxDto.setTaxName(t.getTaxName());
			taxDto.setTaxPercentage(t.getTaxPercentage());
			taxDto.setTaxAmount(t.getTaxAmount());
			return taxDto;
		}).collect(Collectors.toList()));

		return dto;
	}
}
