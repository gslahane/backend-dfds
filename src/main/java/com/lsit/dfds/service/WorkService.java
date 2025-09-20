package com.lsit.dfds.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.HadpWorkDetailsDto;
import com.lsit.dfds.dto.TaxDetailDto;
import com.lsit.dfds.dto.VendorLiteDto;
import com.lsit.dfds.dto.WorkByResponseDto;
import com.lsit.dfds.dto.WorkListFilter;
import com.lsit.dfds.dto.WorkListFundDemands;
import com.lsit.dfds.dto.WorkListRowDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.TaxDetail;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkService {

	private final UserRepository userRepository;
	private final WorkRepository workRepository;
	private final FundDemandRepository fundDemandRepository;
	private final MLCRepository mlcRepository;

	private WorkByResponseDto toDto(Work w) {
		WorkByResponseDto dto = new WorkByResponseDto();
		dto.setId(w.getId());
		dto.setWorkCode(w.getWorkCode());
		dto.setWorkName(w.getWorkName());
		dto.setAdminApprovedAmount(w.getAdminApprovedAmount());
		dto.setSchemeName(w.getScheme() != null ? w.getScheme().getSchemeName() : null);
		dto.setDistrictName(w.getDistrict() != null ? w.getDistrict().getDistrictName() : null);
		dto.setIaName(w.getImplementingAgency() != null ? w.getImplementingAgency().getFullname() : null);
		dto.setMlaName(w.getRecommendedByMla() != null ? w.getRecommendedByMla().getMlaName() : null);
		dto.setMlcName(w.getRecommendedByMlc() != null ? w.getRecommendedByMlc().getMlcName() : null);
		dto.setStatus(w.getStatus());
		dto.setSanctionDate(w.getSanctionDate());
		dto.setCreatedAt(w.getCreatedAt());
		dto.setRecommendationLetterUrl(w.getRecommendationLetterUrl());
		dto.setConstituencyName(w.getConstituency() != null ? w.getConstituency().getConstituencyName() : null);
		dto.setRecommendedAmount(w.getRecommendedAmount());
		dto.setAdminApprovedLetterUrl(w.getAdminApprovedletterUrl());
		return dto;
	}

	public List<WorkByResponseDto> getByMlaId(Long mlaId) {
		return workRepository.findByRecommendedByMla_Id(mlaId).stream().map(this::toDto).collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getByMlcId(Long mlcId, String username) {
		User user = userRepository.findByUsername(username)
			.orElseThrow(() -> new RuntimeException("User not found: " + username));

		MLC userMlc = mlcRepository.findByUser_Id(user.getId())
			.orElseThrow(() -> new RuntimeException("User is not mapped to any MLC"));

		if (!Objects.equals(userMlc.getId(), mlcId)) {
			throw new RuntimeException("Unauthorized: MLC id does not match logged-in user");
		}
		return workRepository.findByRecommendedByMlc_Id(mlcId).stream().map(this::toDto).collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getBySchemeId(Long schemeId) {
		return workRepository.findByScheme_Id(schemeId).stream().map(this::toDto).collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getByIaId(Long iaUserId) {
		return workRepository.findByImplementingAgency_Id(iaUserId).stream().map(this::toDto)
				.collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getByHadpId(Long hadpId) {
		return workRepository.findWorksForHadpDashboard(hadpId, null, null, null, null).stream().map(this::toDto)
				.collect(Collectors.toList());
	}

	// New methods for the updated controller
	public List<WorkByResponseDto> getByDistrictId(Long districtId) {
		return workRepository.findByDistrict_Id(districtId).stream().map(this::toDto).collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getApprovedDemands(Long districtId, Long mlaId, Long mlcId) {
		// Get all works with AA_APPROVED status
		List<Work> approvedWorks = workRepository.findByStatus(com.lsit.dfds.enums.WorkStatuses.AA_APPROVED);

		return approvedWorks.stream().filter(w -> {
			if (districtId != null && w.getDistrict() != null) {
				return w.getDistrict().getId().equals(districtId);
			}
			if (mlaId != null && w.getRecommendedByMla() != null) {
				return w.getRecommendedByMla().getId().equals(mlaId);
			}
			if (mlcId != null && w.getRecommendedByMlc() != null) {
				return w.getRecommendedByMlc().getId().equals(mlcId);
			}
			return true;
		}).map(this::toDto).collect(Collectors.toList());
	}

	public List<WorkByResponseDto> getAllWorksWithFilters(Long districtId, Long mlaId, Long mlcId, String status) {
		// This would need a custom query in the repository
		// For now, returning all works with basic filtering
		return workRepository.findAll().stream().filter(w -> {
			if (districtId != null && w.getDistrict() != null) {
				return w.getDistrict().getId().equals(districtId);
			}
			if (mlaId != null && w.getRecommendedByMla() != null) {
				return w.getRecommendedByMla().getId().equals(mlaId);
			}
			if (mlcId != null && w.getRecommendedByMlc() != null) {
				return w.getRecommendedByMlc().getId().equals(mlcId);
			}
			if (status != null && w.getStatus() != null) {
				return w.getStatus().name().equals(status);
			}
			return true;
		}).map(this::toDto).collect(Collectors.toList());
	}

	public Object getMlaDashboard(Long mlaId, Long financialYearId) {
		// This would need to be implemented with proper dashboard logic
		// For now, returning basic work data
		List<WorkByResponseDto> works = getByMlaId(mlaId);
		return Map.of("works", works, "totalWorks", (long) works.size());
	}

	public Object getMlcDashboard(Long mlcId, Long financialYearId, String username) {
		// This would need to be implemented with proper dashboard logic
		// For now, returning basic work data
		List<WorkByResponseDto> works = getByMlcId(mlcId , username);
		return Map.of("works", works, "totalWorks", (long) works.size());
	}

	public Object getStateDashboard(Long financialYearId, String planType) {
		// This would need to be implemented with proper dashboard logic
		// For now, returning basic work data
		List<WorkByResponseDto> works = getAllWorksWithFilters(null, null, null, null);
		return Map.of("works", works, "totalWorks", (long) works.size());
	}

	public List<WorkByResponseDto> getAllWithFilters(Long mlaId, Long mlcId, Long hadpId, Long schemeId, Long iaUserId,
			Long districtId, String search) {
		// You can call findWorksWithFilters() or write your custom query in repo
		return workRepository.findWorksWithFilters(null, schemeId, districtId, iaUserId, search).stream()
				.map(this::toDto).collect(Collectors.toList());
	}

	/**
	 * Fetches the works for the user based on their role and filters.
	 *
	 * @param username - Username of the logged-in user
	 * @param filter   - WorkListFilter containing filter parameters
	 * @return List of WorkDto objects
	 */
	public List<WorkListRowDto> listWorksForUser(String username, WorkListFilter filter) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		// Always restrict to vendor-assigned + status WORK_ORDER_ISSUED
		List<Work> works = switch (user.getRole()) {
		case STATE_ADMIN, STATE_CHECKER, STATE_MAKER ->
			workRepository.findByVendorIsNotNullAndStatus(WorkStatuses.WORK_ORDER_ISSUED);

		case DISTRICT_COLLECTOR, DISTRICT_DPO, DISTRICT_ADPO, DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER -> {
			if (user.getDistrict() != null) {
				yield workRepository.findByDistrict_IdAndVendorIsNotNullAndStatus(user.getDistrict().getId(),
						WorkStatuses.WORK_ORDER_ISSUED);
			} else {
				yield List.<Work>of();
			}
		}

		case IA_ADMIN -> workRepository.findByImplementingAgency_IdAndVendorIsNotNullAndStatus(user.getId(),
				WorkStatuses.WORK_ORDER_ISSUED);

		case MLA, MLA_REP -> {
			// Works recommended by that MLA user (and already vendor-assigned + W.O.
			// issued)
			// NOTE: user.id != mla.id if you have separate MLA entity; adapt if needed.
			yield workRepository.findByRecommendedByMla_Id(user.getId()).stream().filter(w -> w.getVendor() != null)
					.filter(w -> w.getStatus() == WorkStatuses.WORK_ORDER_ISSUED).toList();
		}

		case MLC -> {
			yield workRepository.findByRecommendedByMlc_Id(user.getId()).stream().filter(w -> w.getVendor() != null)
					.filter(w -> w.getStatus() == WorkStatuses.WORK_ORDER_ISSUED).toList();
		}

		default -> List.of();
		};

		// Apply additional filters
		works = applyFilters(works, filter);

		// Map to rows + demand details
		return works.stream().map(this::toRowDtoWithDemands).collect(Collectors.toList());
	}

	private List<Work> applyFilters(List<Work> works, WorkListFilter filter) {
		if (filter == null)
			return works;

		return works.stream()
				.filter(w -> filter.getDistrictId() == null
						|| (w.getDistrict() != null && Objects.equals(w.getDistrict().getId(), filter.getDistrictId())))
				.filter(w -> filter.getConstituencyId() == null || (w.getConstituency() != null
						&& Objects.equals(w.getConstituency().getId(), filter.getConstituencyId())))
				.filter(w -> filter.getPlanType() == null
						|| (w.getScheme() != null && w.getScheme().getPlanType() == filter.getPlanType()))
				.filter(w -> filter.getFinancialYearId() == null || (w.getFinancialYear() != null
						&& Objects.equals(w.getFinancialYear().getId(), filter.getFinancialYearId())))
				.filter(w -> filter.getSchemeId() == null
						|| (w.getScheme() != null && Objects.equals(w.getScheme().getId(), filter.getSchemeId())))
				.filter(w -> filter.getSearch() == null
						|| (w.getWorkName() != null
								&& w.getWorkName().toLowerCase().contains(filter.getSearch().toLowerCase()))
						|| (w.getWorkCode() != null
								&& w.getWorkCode().toLowerCase().contains(filter.getSearch().toLowerCase())))
				.collect(Collectors.toList());
	}

	private WorkListRowDto toRowDtoWithDemands(Work w) {
		var scheme = w.getScheme();
		var fy = w.getFinancialYear();
		var dist = w.getDistrict();

		WorkListRowDto.WorkListRowDtoBuilder b = WorkListRowDto.builder().workId(w.getId()).workCode(w.getWorkCode())
				.workName(w.getWorkName()).financialYearId(fy != null ? fy.getId() : null)
				.financialYear(fy != null ? fy.getFinacialYear() : null)
				.planType(scheme != null ? scheme.getPlanType() : null).schemeId(scheme != null ? scheme.getId() : null)
				.schemeName(scheme != null ? scheme.getSchemeName() : null)
				.districtId(dist != null ? dist.getId() : null)
				.districtName(dist != null ? dist.getDistrictName() : null)
				.adminApprovedAmount(w.getAdminApprovedAmount()).workPortionAmount(w.getWorkPortionAmount())
				.workPortionTax(w.getWorkPortionTax()).grossAmount(w.getGrossAmount())
				.balanceAmount(w.getBalanceAmount()).sanctionDate(w.getSanctionDate())
				.workOrderLetterUrl(w.getWorkOrderLetterUrl()).status(w.getStatus());

		if (w.getImplementingAgency() != null) {
			b.iaUserId(w.getImplementingAgency().getId()).iaFullname(w.getImplementingAgency().getFullname());
		}

		if (w.getVendor() != null) {
			b.vendor(VendorLiteDto.builder().id(w.getVendor().getId()).name(w.getVendor().getName())
					.gstNumber(w.getVendor().getGstNumber()).panNumber(w.getVendor().getPanNumber())
					.contactPerson(w.getVendor().getContactPerson()).phone(w.getVendor().getPhone())
					.email(w.getVendor().getEmail()).build());
		}

		if (scheme != null) {
			PlanType pt = scheme.getPlanType();
			switch (pt) {
			case MLA -> {
				if (w.getRecommendedByMla() != null) {
					b.mlaId(w.getRecommendedByMla().getId()).mlaName(w.getRecommendedByMla().getMlaName());
				}
				if (w.getConstituency() != null) {
					b.constituencyId(w.getConstituency().getId())
							.constituencyName(w.getConstituency().getConstituencyName());
				}
			}
			case MLC -> {
				if (w.getRecommendedByMlc() != null) {
					b.mlcId(w.getRecommendedByMlc().getId()).mlcName(w.getRecommendedByMlc().getMlcName());
				}
			}
			case HADP -> {
				if (w.getHadp() != null) {
					b.hadpId(w.getHadp().getId());
					if (w.getHadp().getTaluka() != null) {
						b.talukaId(w.getHadp().getTaluka().getId()).talukaName(w.getHadp().getTaluka().getTalukaName());
					}
				}
			}
			default -> {
			}
			}
		}

		// attach all past+present demand details for this work
		List<WorkListFundDemands> demandDtos = fundDemandRepository.findByWork_IdOrderByDemandDateDesc(w.getId())
				.stream().map(this::toDemandDto).toList();

		b.demands(demandDtos);

		return b.build();
	}

	private WorkListFundDemands toDemandDto(FundDemand d) {
		return WorkListFundDemands.builder().id(d.getId()).demandId(d.getDemandId()).amount(nz(d.getAmount()))
				.netPayable(nz(d.getNetPayable())).demandDate(d.getDemandDate())
				.financialYearId(d.getFinancialYear() != null ? d.getFinancialYear().getId() : null)
				.financialYear(d.getFinancialYear() != null ? d.getFinancialYear().getFinacialYear() : null)
				.status(d.getStatus()).remarks(d.getRemarks())
				.taxes(d.getTaxes() != null ? d.getTaxes().stream().map(this::toTaxDto).toList() : List.of()).build();
	}

	private TaxDetailDto toTaxDto(TaxDetail t) {
		TaxDetailDto dto = new TaxDetailDto();
		dto.setTaxName(t.getTaxName());
		dto.setTaxPercentage(t.getTaxPercentage());
		dto.setTaxAmount(t.getTaxAmount());
		return dto;
	}

	private double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	
}