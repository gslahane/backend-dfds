package com.lsit.dfds.service;

import static com.lsit.dfds.repo.WorkSpecs.districtIdIs;
import static com.lsit.dfds.repo.WorkSpecs.fyIdIs;
import static com.lsit.dfds.repo.WorkSpecs.iaIs;
import static com.lsit.dfds.repo.WorkSpecs.planTypeIs;
import static com.lsit.dfds.repo.WorkSpecs.schemeIdIs;
import static com.lsit.dfds.repo.WorkSpecs.statusIs;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.AssignVendorRequestDto;
import com.lsit.dfds.dto.IaDashboardCountsDto;
import com.lsit.dfds.dto.PagedResponse;
import com.lsit.dfds.dto.WorkRowDto;
import com.lsit.dfds.entity.TaxDetail;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;
import com.lsit.dfds.repo.WorkRepository;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkVendorMappServiceImpl implements WorkVendorMappService {

	private final UserRepository userRepo;
	private final WorkRepository workRepo;
	private final VendorRepository vendorRepo;
	private final JwtUtil jwtUtil;
	private final FileStorageService fileStorageService; // <-- inject

	private final SchemesRepository schemeRepo;

	@Override
	public List<Work> getWorksForIAUser(String username, String financialYear, Long schemeId, String status) {
		User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Long districtId = user.getSupervisor() != null ? user.getSupervisor().getId() : null;

		return workRepo.findAll().stream().filter(work -> work.getScheme() != null)
				.filter(work -> districtId == null
						|| work.getScheme().getDistricts().stream().anyMatch(d -> d.getId().equals(districtId)))
				.filter(work -> financialYear == null || financialYear.equals("All")
						|| work.getFinancialYear().equals(financialYear))
				.filter(work -> schemeId == null || schemeId == 0L || work.getScheme().getId().equals(schemeId))
				.filter(work -> {
					if ("Pending".equalsIgnoreCase(status)) {
						return work.getVendor() == null;
					}
					if ("Completed".equalsIgnoreCase(status)) {
						return work.getVendor() != null;
					}
					return true;
				}).collect(Collectors.toList());
	}

	@Override
	public List<Vendor> getVendorsForIA(String username) {
		User iaUser = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		return vendorRepo.findAll().stream().filter(v -> v.getCreatedBy() != null && v.getCreatedBy().equals(username))
				.collect(Collectors.toList());
	}

	@Override
	@Transactional
	public Work assignVendorToWork(AssignVendorRequestDto dto, String username, MultipartFile workOrderFile) {
		// (Optional) authorization check — ensure only IA/District/State roles do this
		User actor = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Work work = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));

		Vendor vendor = vendorRepo.findById(dto.getVendorId())
				.orElseThrow(() -> new RuntimeException("Vendor not found"));

		// Map vendor + amounts
		work.setVendor(vendor);

		// Amounts & taxes
		double portion = dto.getWorkPortionAmount() != null ? dto.getWorkPortionAmount() : 0.0;
		List<TaxDetail> taxLines = (dto.getTaxes() == null) ? List.of() : dto.getTaxes().stream().map(t -> {
			TaxDetail d = new TaxDetail();
			d.setTaxName(t.getTaxName());
			d.setTaxPercentage(t.getTaxPercentage());
			d.setTaxAmount(t.getTaxAmount());
			return d;
		}).collect(Collectors.toList());

		work.setAppliedTaxes(taxLines);

		double totalTax = taxLines.stream().mapToDouble(TaxDetail::getTaxAmount).sum();
		work.setWorkPortionAmount(portion);
		work.setWorkPortionTax(totalTax);

		// Gross: use provided if present, else compute portion + tax
		double gross = (dto.getGrossAmount() != null) ? dto.getGrossAmount() : (portion + totalTax);
		work.setGrossAmount(gross);

		// Balance (if you want to set initial balance = gross at vendor mapping stage)
		work.setBalanceAmount(gross);

		// Save work order file -> URL
		if (workOrderFile != null && !workOrderFile.isEmpty()) {
			String url = fileStorageService.saveFile(workOrderFile, "work-orders");
			work.setWorkOrderLetterUrl(url);
		} else if (dto.getWorkOrderLetterUrl() != null && !dto.getWorkOrderLetterUrl().isBlank()) {
			// Accept pre-existing URL if no file uploaded
			work.setWorkOrderLetterUrl(dto.getWorkOrderLetterUrl());
		}

		// Optional: set status to WORK_ORDER_ISSUED at this point
		work.setStatus(WorkStatuses.WORK_ORDER_ISSUED);

		return workRepo.save(work);
	}

	@Override
	@Transactional
	public IaDashboardCountsDto getDashboardCounts(String username) {
		User iaUser = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		if (iaUser.getRole() != Roles.IA_ADMIN) {
			throw new RuntimeException("Unauthorized. Only IA_ADMIN can view counts.");
		}
		if (iaUser.getDistrict() == null) {
			throw new RuntimeException("District not mapped to IA user.");
		}

		Long districtId = iaUser.getDistrict().getId();

		long totalAaWorks = workRepo.countByDistrict_IdAndAdminApprovedAmountGreaterThan(districtId, 0.0);
		double totalAdminApprovedAmount = workRepo.sumAdminApprovedAmountByDistrict(districtId);

		long totalWorksAssignedWithVendors = workRepo.countByDistrict_IdAndVendorIsNotNull(districtId);

		long pendingWorkVendorRegistration = workRepo
				.countByDistrict_IdAndAdminApprovedAmountGreaterThanAndVendorIsNull(districtId, 0.0);

		return IaDashboardCountsDto.builder().totalAaWorks(totalAaWorks)
				.totalAdminApprovedAmount(totalAdminApprovedAmount)
				.totalWorksAssignedWithVendors(totalWorksAssignedWithVendors)
				.pendingWorkVendorRegistration(pendingWorkVendorRegistration).build();
	}

	@Override
	public PagedResponse<WorkRowDto> getAaApprovedWorks(String username, PlanType planType, Long financialYearId,
			Long schemeId, Integer page, Integer size) {
		var user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// Scope
		Long districtIdScope = null;
		Long iaUserScope = null;

		switch (user.getRole()) {
		case IA_ADMIN -> {
			// IA sees their own assigned works (and naturally in their district).
			iaUserScope = user.getId();
		}
		case DISTRICT_ADMIN, DISTRICT_ADPO, DISTRICT_DPO, DISTRICT_COLLECTOR -> {
			if (user.getDistrict() == null) {
				throw new RuntimeException("District not mapped for user");
			}
			districtIdScope = user.getDistrict().getId();
		}
		case STATE_ADMIN, STATE_CHECKER, STATE_MAKER -> {
			// full state scope; no district restriction
		}
		default -> throw new RuntimeException("Role not allowed to view AA-approved works");
		}

		// Build filters
		Specification<Work> spec = Specification.where(statusIs(WorkStatuses.AA_APPROVED)).and(planTypeIs(planType))
				.and(fyIdIs(financialYearId)).and(schemeIdIs(schemeId)).and(districtIdIs(districtIdScope))
				.and(iaIs(iaUserScope));

		Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 50,
				Sort.by(Sort.Direction.DESC, "sanctionDate", "id"));

		Page<Work> result = workRepo.findAll(spec, pageable);

		List<WorkRowDto> rows = result.getContent().stream().map(w -> {
			var scheme = w.getScheme();
			var fy = w.getFinancialYear();
			var dist = w.getDistrict();

			WorkRowDto.WorkRowDtoBuilder b = WorkRowDto.builder().workId(w.getId()).workCode(w.getWorkCode())
					.workName(w.getWorkName()).financialYearId(fy != null ? fy.getId() : null)
					.financialYear(fy != null ? fy.getFinacialYear() : null)
					.planType(scheme != null ? scheme.getPlanType() : null)
					.schemeId(scheme != null ? scheme.getId() : null)
					.schemeName(scheme != null ? scheme.getSchemeName() : null)
					.districtId(dist != null ? dist.getId() : null)
					.districtName(dist != null ? dist.getDistrictName() : null)
					.adminApprovedAmount(w.getAdminApprovedAmount())
					.adminApprovalLetterUrl(w.getAdminApprovedletterUrl()).sanctionDate(w.getSanctionDate())
					.status(w.getStatus());

			if (w.getImplementingAgency() != null) {
				b.iaUserId(w.getImplementingAgency().getId()).iaFullname(w.getImplementingAgency().getFullname());
			}
			if (w.getVendor() != null) {
				b.vendorId(w.getVendor().getId()).vendorName(w.getVendor().getName());
			}

			// Enrich per plan-type
			if (scheme != null) {
				switch (scheme.getPlanType()) {
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
							b.talukaId(w.getHadp().getTaluka().getId())
									.talukaName(w.getHadp().getTaluka().getTalukaName());
						}
					}
				}
				default -> {
					/* other plan types if any */ }
				}
			}

			return b.build();
		}).toList();

		return new PagedResponse<>(rows, result.getTotalElements(), result.getTotalPages(), result.getNumber(),
				result.getSize());
	}

}
