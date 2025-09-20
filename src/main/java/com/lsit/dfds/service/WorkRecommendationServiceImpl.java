package com.lsit.dfds.service;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.HADPRecommendWorkReqDto;
import com.lsit.dfds.dto.HadpRecomendWorkResponseDto;
import com.lsit.dfds.dto.HadpWorkDetailsDto;
import com.lsit.dfds.dto.MLARecomendWorkReqDto;
import com.lsit.dfds.dto.MLARecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecommendWorkReqDto;
import com.lsit.dfds.dto.TaxDetailDto;
import com.lsit.dfds.dto.WorkAaApprovalDto;
import com.lsit.dfds.dto.WorkApprovalDto;
import com.lsit.dfds.dto.WorkDetailsWithStatusDto;
import com.lsit.dfds.dto.WorkListFilter;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.entity.HADPBudget;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.entity.TaxDetail;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictLimitAllocationRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.FundTransferRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkRecommendationServiceImpl implements WorkRecommendationService {

	private final UserRepository userRepo;
	private final WorkRepository workRepo;
	private final MLARepository mlaRepo;
	private final MLCRepository mlcRepo;
	private final HADPRepository hadpRepo;
	private final MLABudgetRepository mlaBudgetRepo;
	private final MLCBudgetRepository mlcBudgetRepo;
	private final HADPBudgetRepository hadpBudgetRepo;
	private final DistrictLimitAllocationRepository dlaRepo;
	private final SchemeBudgetRepository schemeBudgetRepo;
	private final DistrictRepository districtRepo;
	private final ConstituencyRepository constituencyRepo;
	private final TalukaRepository talukaRepo;
	private final SchemesRepository schemesRepo;
	private final FinancialYearRepository fyRepo;
	private final DemandCodeRepository demandCodeRepo;
	private final VendorRepository vendorRepo;
	private final FundDemandRepository fundDemandRepo;
	private final FundTransferRepository fundTransferRepository;
	private final MLATermRepository mlaTermRepository;

	private final FileStorageService fileStorageService;

	// -------------------- Helpers --------------------
	private User user(String username) {
		return userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found: " + username));
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	private boolean isStateUser(Roles role) {
		return role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER;
	}

	private boolean isDistrictUser(Roles role) {
		return role == Roles.DISTRICT_COLLECTOR || role == Roles.DISTRICT_DPO || role == Roles.DISTRICT_ADPO
				|| role == Roles.DISTRICT_ADMIN || role == Roles.DISTRICT_CHECKER || role == Roles.DISTRICT_MAKER;
	}

	private Schemes readSingleSchemeField(Object owner, String... fieldNames) {
		for (String name : fieldNames) {
			try {
				Field f = owner.getClass().getDeclaredField(name);
				f.setAccessible(true);
				Object v = f.get(owner);
				if (v instanceof Schemes s) {
					return s;
				}
			} catch (NoSuchFieldException ignored) {
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Cannot access field '" + name + "' on " + owner.getClass().getSimpleName(),
						e);
			}
		}
		return null;
	}

	private Schemes resolveDefaultSchemeForMla(MLA mla) {
		Schemes s = readSingleSchemeField(mla, "defaultScheme", "scheme");
		if (s == null || s.getId() == null) {
			throw new RuntimeException("MLA default scheme is not configured");
		}
		return s;
	}

	private Schemes resolveDefaultSchemeForMlc(MLC mlc) {
		Schemes s = schemesRepo.findById(7L).orElse(null);
		if (s == null || s.getId() == null) {
			throw new RuntimeException("MLC default scheme is not configured");
		}
		return s;
	}

	private static List<TaxDetail> mapTaxes(List<TaxDetailDto> dtos) {
		if (dtos == null || dtos.isEmpty())
			return List.of();
		List<TaxDetail> out = new ArrayList<>();
		for (TaxDetailDto t : dtos) {
			TaxDetail td = new TaxDetail();
			td.setTaxName(t.getTaxName());
			td.setTaxPercentage(nz(t.getTaxPercentage()));
			td.setTaxAmount(nz(t.getTaxAmount()));
			out.add(td);
		}
		return out;
	}

	private static <T> T firstOrThrow(List<T> rows, String message) {
		if (rows == null || rows.isEmpty()) {
			throw new RuntimeException(message);
		}
		return rows.get(0);
	}

	// -------- Budget checks --------
	private void ensureMlaBudgetAllows(Long mlaId, Long fyId, double amount) {
		List<MLABudget> rows = mlaBudgetRepo.findByMla_IdAndFinancialYear_Id(mlaId, fyId);
		MLABudget b = firstOrThrow(rows, "MLA Budget row not found");
		double remaining = nz(b.getAllocatedLimit()) - nz(b.getUtilizedLimit());
		if (amount <= 0)
			throw new RuntimeException("Recommended amount must be > 0");
		if (amount > remaining) {
			throw new RuntimeException(
					"Recommended amount exceeds MLA remaining FY budget (remaining=" + remaining + ")");
		}
	}

	private void ensureMlcBudgetAllows(Long mlcId, Long fyId, double amount) {
		List<MLCBudget> rows = mlcBudgetRepo.findByMlc_IdAndFinancialYear_Id(mlcId, fyId);
		MLCBudget b = firstOrThrow(rows, "MLC Budget row not found");
		double remaining = nz(b.getAllocatedLimit()) - nz(b.getUtilizedLimit());
		if (amount <= 0)
			throw new RuntimeException("Recommended amount must be > 0");
		if (amount > remaining) {
			throw new RuntimeException(
					"Recommended amount exceeds MLC remaining FY budget (remaining=" + remaining + ")");
		}
	}

    private HADP hadpRowForTaluka(Long districtId, Long talukaId, Long fyId) {
        List<HADP> all = hadpRepo.findAll();
        System.out.println(">>> Looking for District=" + districtId + ", Taluka=" + talukaId + ", FY=" + fyId);
        for (HADP h : all) {
            System.out.println("HADP row: id=" + h.getId()
                    + " district=" + (h.getDistrict()!=null ? h.getDistrict().getId() : null)
                    + " taluka=" + (h.getTaluka()!=null ? h.getTaluka().getId() : null)
                    + " fy=" + (h.getFinancialYear()!=null ? h.getFinancialYear().getId() : null)
                    + " status=" + h.getStatus());
        }

        return all.stream()
                .filter(h -> h.getDistrict() != null && Objects.equals(h.getDistrict().getId(), districtId))
                .filter(h -> h.getTaluka() != null && h.getTaluka().getId().equals(talukaId))
                .filter(h -> h.getFinancialYear() != null && h.getFinancialYear().getId().equals(fyId))
                .filter(h -> h.getStatus() == null || h.getStatus() == Statuses.ACTIVE)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Taluka is not HADP-enrolled for this FY in this district"));
    }


    private void ensureHadpTalukaBudgetAllows(Long districtId, Long talukaId, Long fyId, Long schemeId, double amount) {
		List<HADPBudget> all = hadpBudgetRepo.findAll();
		HADPBudget budget = all.stream()
				.filter(b -> b.getDistrict() != null && Objects.equals(b.getDistrict().getId(), districtId))
				.filter(b -> b.getTaluka() != null && Objects.equals(b.getTaluka().getId(), talukaId))
				.filter(b -> b.getFinancialYear() != null && Objects.equals(b.getFinancialYear().getId(), fyId))
				.filter(b -> schemeId == null
						|| (b.getScheme() != null && Objects.equals(b.getScheme().getId(), schemeId)))
				.findFirst().orElseThrow(() -> new RuntimeException("HADP Taluka budget row not found"));

		double remaining = nz(budget.getAllocatedLimit()) - nz(budget.getUtilizedLimit());
		if (amount <= 0)
			throw new RuntimeException("Recommended amount must be > 0");
		if (amount > remaining) {
			throw new RuntimeException(
					"Recommended amount exceeds HADP Taluka remaining FY budget (remaining=" + remaining + ")");
		}
	}

	// -------------------- MLA recommend --------------------
	@Override
	@Transactional
	public MLARecomendWorkResponseDto recommendByMla(MLARecomendWorkReqDto dto, String username, String workCode,
			String recommendationLetterUrl) {
		User authUser = userRepo.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Authenticated user not found: " + username));
		if (authUser.getRole() != Roles.MLA) {
			throw new RuntimeException("Only MLA can recommend works");
		}

		MLA mla = mlaRepo.findByUser_Id(authUser.getId()).orElse(null);
		if (mla == null) {
			Long fallback = dto.getMlaid();
			if (fallback == null) {
				throw new RuntimeException("Logged-in user is not mapped to an MLA and request did not include mlaid");
			}
			mla = mlaRepo.findById(fallback)
					.orElseThrow(() -> new RuntimeException("MLA not found for ID: " + fallback));
		}

		// Permissible scope logic
		MLATerm mlaTerm = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId()).orElse(null);
		if (mlaTerm != null) {
			// Calculate permissible scope
			List<MLABudget> mlaBudgets = mlaBudgetRepo.findByMla_Id(mla.getId());
			double totalAllocated = mlaBudgets.stream().mapToDouble(b -> nz(b.getAllocatedLimit())).sum();
			double totalUtilized = mlaBudgets.stream().mapToDouble(b -> nz(b.getUtilizedLimit())).sum();
			double permissibleScope;
			if (mlaTerm.getTenureEndDate() != null) {
				java.time.LocalDate today = java.time.LocalDate.now();
				long yearsAhead = java.time.temporal.ChronoUnit.YEARS.between(today, mlaTerm.getTenureEndDate());
				if (yearsAhead >= 1) {
					permissibleScope = (totalAllocated * 1.5) - totalUtilized;
				} else {
					permissibleScope = totalAllocated - totalUtilized;
				}
			} else {
				permissibleScope = totalAllocated - totalUtilized;
			}
			// Calculate total recommended so far
			double totalRecommended = workRepo.findByRecommendedByMla_Id(mla.getId()).stream()
					.mapToDouble(w -> nz(w.getRecommendedAmount())).sum();
			// Check if new recommendation exceeds permissible scope
			if (totalRecommended + nz(dto.getRecommendedAmount()) > permissibleScope) {
				throw new RuntimeException(
						"MLA cannot recommend work beyond permissible scope (limit=" + permissibleScope + ")");
			}
		}

		// Always use MLA's default scheme; ignore any schemeId from request
		Schemes scheme = resolveDefaultSchemeForMla(mla);
		if (scheme == null || scheme.getId() == null) {
			throw new RuntimeException("MLA default scheme is not configured/persisted");
		}
		if (scheme.getPlanType() != PlanType.MLA) {
			throw new RuntimeException("Default scheme for MLA must have PlanType=MLA");
		}

		// Financial year strictly from the scheme; ignore any financialYearId from
		// request
		/*
		 * FinancialYear financialYear = scheme.getFinancialYear(); if (financialYear ==
		 * null || financialYear.getId() == null) { throw new
		 * RuntimeException("Default MLA scheme has no financial year configured"); }
		 */

		double recommended = nz(dto.getRecommendedAmount());
		// Budget guard: MLA budget remaining for FY
		ensureMlaBudgetAllows(mla.getId(), dto.getFinancialYearId(), recommended);

		// Optional IA
		User implementingAgency = null;
		if (dto.getImplementingAgencyUserId() != null) {
			implementingAgency = userRepo.findById(dto.getImplementingAgencyUserId()).orElseThrow(
					() -> new RuntimeException("Implementing agency not found: " + dto.getImplementingAgencyUserId()));
		}

		// Build Work (recommendation stage only)
		Work work = new Work();
		work.setWorkName(dto.getWorkName());
		work.setWorkCode(workCode);
		work.setRecommendedAmount(recommended);
		work.setDistrictApprovedAmount(0.0);
		work.setAdminApprovedAmount(0.0);
		work.setGrossAmount(0.0);
		work.setBalanceAmount(0.0);
		work.setStatus(WorkStatuses.PROPOSED);
		work.setRecommendationLetterUrl(recommendationLetterUrl);
		work.setCreatedBy(username);
		work.setDescription(dto.getDescription());
		if (mla.getConstituency() == null || mla.getConstituency().getDistrict() == null) {
			throw new RuntimeException("MLA does not have a valid constituency/district.");
		}

		work.setDistrict(mla.getConstituency().getDistrict());
		work.setConstituency(mla.getConstituency());
		work.setScheme(scheme);
		FinancialYear financialYear = fyRepo.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial year not found: " + dto.getFinancialYearId()));
		work.setFinancialYear(financialYear);
		work.setRecommendedByMla(mla);
		if (implementingAgency != null) {
			work.setImplementingAgency(implementingAgency);
		}

		// Do NOT compute taxes/work-portion at recommendation
		// (If you want to persist user-provided lines only:)
		if (dto.getTaxes() != null && !dto.getTaxes().isEmpty()) {
			work.setAppliedTaxes(mapTaxes(dto.getTaxes())); // store lines, but no math
		}

		work = workRepo.save(work);
		return toMlaResponse(work);
	}

	private MLARecomendWorkResponseDto toMlaResponse(Work work) {
		MLARecomendWorkResponseDto dto = new MLARecomendWorkResponseDto();
		dto.setId(work.getId());
		dto.setWorkName(work.getWorkName());
		dto.setWorkCode(work.getWorkCode());
		dto.setRecommendedAmount(work.getRecommendedAmount()); // NEW
		dto.setDistrictApprovedAmount(work.getDistrictApprovedAmount()); // NEW
		dto.setGrossAmount(work.getGrossAmount());
		dto.setBalanceAmount(work.getBalanceAmount());
		dto.setAdminApprovedAmount(work.getAdminApprovedAmount());
		dto.setStatus(work.getStatus());
		dto.setDescription(work.getDescription());
		dto.setDistrictId(work.getDistrict() != null ? work.getDistrict().getId() : null);
		dto.setConstituencyId(work.getConstituency() != null ? work.getConstituency().getId() : null);
		dto.setDistrictName(work.getDistrict() != null ? work.getDistrict().getDistrictName() : null);
		dto.setConstituencyName(work.getConstituency() != null ? work.getConstituency().getConstituencyName() : null);
		dto.setSchemeName(work.getScheme() != null ? work.getScheme().getSchemeName() : null);
		dto.setImplementingAgencyName(
				work.getImplementingAgency() != null ? work.getImplementingAgency().getFullname() : null);
		dto.setMlaName(work.getRecommendedByMla() != null ? work.getRecommendedByMla().getMlaName() : null);
		dto.setRecommendationLetterUrl(work.getRecommendationLetterUrl());
		return dto;
	}

	// in WorkRecommendationServiceImpl

	@Override
	@Transactional
	public MLCRecomendWorkResponseDto recommendByMlc(MLCRecommendWorkReqDto dto, String username,
			String recommendationLetterUrl) {
		User u = user(username);
		if (u.getRole() != Roles.MLC)
			throw new RuntimeException("Only MLC can recommend MLC works");

		MLC mlc = mlcRepo.findByUser_Id(u.getId()).orElseThrow(() -> new RuntimeException("MLC record not linked"));

		if (dto.getTargetDistrictId() == null)
			throw new RuntimeException("targetDistrictId is required for MLC works");
		District target = districtRepo.findById(dto.getTargetDistrictId())
				.orElseThrow(() -> new RuntimeException("Target district not found"));

		boolean nodal = (u.getDistrict() != null) && Objects.equals(u.getDistrict().getId(), target.getId());

		Schemes scheme = resolveDefaultSchemeForMlc(mlc);
		if (scheme == null || scheme.getId() == null)
			throw new RuntimeException("MLC default scheme is not configured/persisted");
		if (scheme.getPlanType() != PlanType.MLC)
			throw new RuntimeException("Default scheme for MLC must have PlanType=MLC");

		if (dto.getFinancialYearId() == null) {
			throw new RuntimeException("Financial year ID is required for MLC recommendation");
		}
		FinancialYear fy = fyRepo.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial year not found: " + dto.getFinancialYearId()));

		double recommended = nz(dto.getRecommendedAmount());
		ensureMlcBudgetAllows(mlc.getId(), fy.getId(), recommended);

		// Optional IA: must belong to target district
		User implementingAgency = null;
		if (dto.getImplementingAgencyUserId() != null) {
			implementingAgency = userRepo.findById(dto.getImplementingAgencyUserId()).orElseThrow(
					() -> new RuntimeException("Implementing agency not found: " + dto.getImplementingAgencyUserId()));
			if (implementingAgency.getRole() != Roles.IA_ADMIN)
				throw new RuntimeException("Implementing agency must be IA_ADMIN");
			if (implementingAgency.getDistrict() == null
					|| !Objects.equals(implementingAgency.getDistrict().getId(), target.getId()))
				throw new RuntimeException("Implementing agency must belong to the target district");
		}

		Work w = new Work();
		w.setWorkName(dto.getWorkName());
		w.setDescription(dto.getDescription());
		w.setRecommendedAmount(recommended);
		w.setDistrictApprovedAmount(0.0);
		w.setGrossAmount(0.0);
		w.setBalanceAmount(0.0);
		w.setAdminApprovedAmount(0.0);
		w.setWorkCode(dto.getWorkCode());

		w.setScheme(scheme);
		w.setFinancialYear(fy);
		w.setDistrict(target);
		w.setRecommendedByMlc(mlc);
		w.setIsNodalWork(nodal);
		w.setStatus(WorkStatuses.PROPOSED);
		w.setCreatedBy(username);
		w.setRemark(nodal ? "Nodal Work" : "Non-Nodal Work");
		w.setRecommendationLetterUrl(recommendationLetterUrl);

		if (dto.getTaxes() != null && !dto.getTaxes().isEmpty()) {
			w.setAppliedTaxes(mapTaxes(dto.getTaxes()));
			w.setWorkPortionTax(0.0);
			w.setWorkPortionAmount(0.0);
		}

		if (implementingAgency != null) {
			w.setImplementingAgency(implementingAgency);
		}

		w = workRepo.save(w);
		return toMlcResponse(w);
	}

	private MLCRecomendWorkResponseDto toMlcResponse(Work w) {
		MLCRecomendWorkResponseDto dto = new MLCRecomendWorkResponseDto();
		dto.setId(w.getId());
		dto.setWorkName(w.getWorkName());
		dto.setWorkCode(w.getWorkCode());
		dto.setRecommendedAmount(w.getRecommendedAmount());
		dto.setDistrictApprovedAmount(w.getDistrictApprovedAmount());
		dto.setGrossAmount(w.getGrossAmount());
		dto.setBalanceAmount(w.getBalanceAmount());
		dto.setStatus(w.getStatus() != null ? w.getStatus() : null); // (optional) use .name()
		dto.setDescription(w.getDescription());
		dto.setDistrictId(w.getDistrict() != null ? w.getDistrict().getId() : null);
		dto.setDistrictName(w.getDistrict() != null ? w.getDistrict().getDistrictName() : null);
		dto.setSchemeName(w.getScheme() != null ? w.getScheme().getSchemeName() : null);
		dto.setIsNodalWork(w.getIsNodalWork());
		dto.setMlcName(w.getRecommendedByMlc() != null ? w.getRecommendedByMlc().getMlcName() : null);
		dto.setRecommendationLetterUrl(w.getRecommendationLetterUrl()); // ✅ now always flows through
		return dto;
	}

	// -------------------- HADP recommend --------------------
	@Override
	@Transactional
	public HadpRecomendWorkResponseDto recommendByHadp(HADPRecommendWorkReqDto dto, String username,
			MultipartFile letter) {

		User u = user(username);

		boolean districtLevel = switch (u.getRole()) {
		case DISTRICT_COLLECTOR, DISTRICT_DPO, DISTRICT_ADPO, DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER -> true;
		case HADP_ADMIN -> true;
		default -> false;
		};

		if (!districtLevel)
			throw new RuntimeException("Only District/HADP roles can recommend HADP works");

		if (u.getDistrict() == null)
			throw new RuntimeException("User must be mapped to a district");

		if (dto.getHadpTalukaId() == null)
			throw new RuntimeException("talukaId is required for HADP works");

		Taluka taluka = talukaRepo.findById(dto.getHadpTalukaId())
				.orElseThrow(() -> new RuntimeException("Taluka not found"));
		District district = taluka.getDistrict();

		if (district == null)
			throw new RuntimeException("District not found for Taluka");

		if (!Objects.equals(district.getId(), u.getDistrict().getId()))
			throw new RuntimeException("Taluka must belong to user's district");

		if (dto.getFinancialYearId() == null)
			throw new RuntimeException("financialYearId is required");

		Long fyId = dto.getFinancialYearId();
		HADP hadpRow = hadpRowForTaluka(district.getId(), taluka.getId(), fyId);

		Schemes scheme = schemesRepo.findByPlanTypeAndFinancialYearId(PlanType.HADP, fyId)
				.orElseThrow(() -> new RuntimeException("HADP scheme not configured for FY " + fyId));

		double recommended = nz(dto.getRecommendedAmount());
		ensureHadpTalukaBudgetAllows(district.getId(), taluka.getId(), fyId, scheme.getId(), recommended);

		Work w = new Work();
		w.setWorkName(dto.getWorkName());
		w.setDescription(dto.getDescription());
		w.setRecommendedAmount(recommended);
		w.setDistrictApprovedAmount(0.0);
		w.setAdminApprovedAmount(0.0);
		w.setGrossAmount(0.0);
		w.setBalanceAmount(0.0);
		w.setScheme(scheme);
		w.setFinancialYear(hadpRow.getFinancialYear());
		w.setDistrict(district);
		w.setHadp(hadpRow);
		w.setConstituency(null);
		w.setIsNodalWork(true);
		w.setStatus(WorkStatuses.ADPO_APPROVED);
		w.setCreatedBy(username);

		if (dto.getTaxes() != null && !dto.getTaxes().isEmpty()) {
			w.setAppliedTaxes(mapTaxes(dto.getTaxes()));
		}

		if (letter != null && !letter.isEmpty()) {
			String url = fileStorageService.saveFile(letter, "hadp-recommendations");
			w.setRecommendationLetterUrl(url);
		}

		w = workRepo.save(w);

		return toHadpResponse(w, taluka);
	}

	private HadpRecomendWorkResponseDto toHadpResponse(Work w, Taluka taluka) {
		HadpRecomendWorkResponseDto dto = new HadpRecomendWorkResponseDto();
		dto.setId(w.getId());
		dto.setWorkName(w.getWorkName());
		dto.setRecommendedAmount(w.getRecommendedAmount());
		dto.setDistrictApprovedAmount(w.getDistrictApprovedAmount());
		dto.setGrossAmount(w.getGrossAmount());
		dto.setBalanceAmount(w.getBalanceAmount());
		dto.setStatus(w.getStatus());
		dto.setDescription(w.getDescription());
		dto.setDistrictId(w.getDistrict() != null ? w.getDistrict().getId() : null);
		dto.setDistrictName(w.getDistrict() != null ? w.getDistrict().getDistrictName() : null);
		dto.setTalukaId(taluka != null ? taluka.getId() : null);
		dto.setTalukaName(taluka != null ? taluka.getTalukaName() : null);
		dto.setSchemeName(w.getScheme() != null ? w.getScheme().getSchemeName() : null);
		dto.setRecommendationLetterUrl(w.getRecommendationLetterUrl());
		return dto;
	}

	@Transactional(readOnly = true)
	public List<HadpWorkDetailsDto> fetchHadpWorks(String username, Long hadpId, Long districtId, Long talukaId,
			Long financialYearId, WorkStatuses status, HadpType hadpType) {

		User u = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		List<Work> base;
		if (isStateUser(u.getRole())) {
			base = workRepo.findAll();
		} else if (isDistrictUser(u.getRole())) {
			if (u.getDistrict() == null)
				return List.of();
			base = workRepo.findByDistrict_Id(u.getDistrict().getId());
		} else if (u.getRole() == Roles.IA_ADMIN) {
			base = workRepo.findByImplementingAgency_Id(u.getId());
		} else {
			return List.of(); // MLA/MLC not allowed for HADP works
		}

		// Apply filters
		return base.stream().filter(w -> w.getScheme() != null && w.getScheme().getPlanType() == PlanType.HADP)
				.filter(w -> hadpId == null || (w.getHadp() != null && Objects.equals(w.getHadp().getId(), hadpId)))
				.filter(w -> districtId == null
						|| (w.getDistrict() != null && Objects.equals(w.getDistrict().getId(), districtId)))
				.filter(w -> talukaId == null || (w.getHadp() != null && w.getHadp().getTaluka() != null
						&& Objects.equals(w.getHadp().getTaluka().getId(), talukaId)))
				.filter(w -> financialYearId == null || (w.getFinancialYear() != null
						&& Objects.equals(w.getFinancialYear().getId(), financialYearId)))
				.filter(w -> status == null || w.getStatus() == status)
				.filter(w -> hadpType == null || (w.getHadp() != null && w.getHadp().getType() == hadpType))
				.map(this::toHadpDto).collect(Collectors.toList());
	}

	private HadpWorkDetailsDto toHadpDto(Work w) {
		return HadpWorkDetailsDto.builder().workId(w.getId()).workName(w.getWorkName()).workCode(w.getWorkCode())
				.description(w.getDescription()).remark(w.getRemark()).status(w.getStatus())
				.workStartDate(w.getWorkStartDate()).workEndDate(w.getWorkEndDate()).sanctionDate(w.getSanctionDate())
				.createdAt(w.getCreatedAt()).createdBy(w.getCreatedBy()).recommendedAmount(w.getRecommendedAmount())
				.districtApprovedAmount(w.getDistrictApprovedAmount()).grossAmount(w.getGrossAmount())
				.adminApprovedAmount(w.getAdminApprovedAmount()).balanceAmount(w.getBalanceAmount())
				.schemeId(w.getScheme() != null ? w.getScheme().getId() : null)
				.schemeName(w.getScheme() != null ? w.getScheme().getSchemeName() : null)
				.planType(w.getScheme() != null ? w.getScheme().getPlanType().name() : null)
				.financialYearId(w.getFinancialYear() != null ? w.getFinancialYear().getId() : null)
				.financialYear(w.getFinancialYear() != null ? w.getFinancialYear().getFinacialYear() : null)
				.districtId(w.getDistrict() != null ? w.getDistrict().getId() : null)
				.districtName(w.getDistrict() != null ? w.getDistrict().getDistrictName() : null)
				.talukaId(
						w.getHadp() != null && w.getHadp().getTaluka() != null ? w.getHadp().getTaluka().getId() : null)
				.talukaName(
						w.getHadp() != null && w.getHadp().getTaluka() != null ? w.getHadp().getTaluka().getTalukaName()
								: null)
				.recommendationLetterUrl(w.getRecommendationLetterUrl())
				.adminApprovedLetterUrl(
						w.getAdminApprovedletterUrl()).workOrderLetterUrl(w.getWorkOrderLetterUrl())
				.build();
	}

	@Override
	@Transactional
	public Work approveAaAndAssign(WorkAaApprovalDto dto, String approverUsername, MultipartFile aaLetterFile) {
		User approver = user(approverUsername);

		boolean allowed = switch (approver.getRole()) {
		case DISTRICT_ADPO, DISTRICT_DPO, DISTRICT_COLLECTOR, DISTRICT_ADMIN -> true;
		case STATE_ADMIN, STATE_CHECKER, STATE_MAKER -> true;
		default -> false;
		};
		if (!allowed)
			throw new RuntimeException("Not authorized to approve AA");

		Work w = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));

		// Assign IA (must be in same district)
		if (dto.getImplementingAgencyUserId() != null) {
			User ia = userRepo.findById(dto.getImplementingAgencyUserId())
					.orElseThrow(() -> new RuntimeException("IA user not found"));
			if (ia.getRole() != Roles.IA_ADMIN)
				throw new RuntimeException("Implementing agency must be IA_ADMIN");
			if (ia.getDistrict() == null || w.getDistrict() == null
					|| !Objects.equals(ia.getDistrict().getId(), w.getDistrict().getId())) {
				throw new RuntimeException("IA must belong to the same district as work");
			}
			w.setImplementingAgency(ia);
		}

		// (Optional) vendor mapping — keep commented if not needed at AA stage
//	    if (dto.getVendorId() != null) {
//	        Vendor v = vendorRepo.findById(dto.getVendorId())
//	                .orElseThrow(() -> new RuntimeException("Vendor not found"));
//	        w.setVendor(v);
//	    }

		// Optional taxes at AA (allowed to compute here)
		if (dto.getTaxes() != null && !dto.getTaxes().isEmpty()) {
			w.setAppliedTaxes(mapTaxes(dto.getTaxes()));
			double taxSum = dto.getTaxes().stream().mapToDouble(t -> nz(t.getTaxAmount())).sum();
			w.setWorkPortionTax(taxSum);
			double base = (dto.getAdminApprovedAmount() != null && dto.getAdminApprovedAmount() > 0)
					? dto.getAdminApprovedAmount()
					: nz(w.getGrossAmount());
			w.setWorkPortionAmount(base - taxSum);
		}

		double aa = nz(dto.getAdminApprovedAmount());
		if (aa <= 0)
			throw new RuntimeException("AA amount must be > 0");

		w.setAdminApprovedAmount(aa);
		w.setSanctionDate(dto.getAaApprovalDate() != null ? dto.getAaApprovalDate() : LocalDate.now());
		w.setWorkStartDate(w.getSanctionDate());
		w.setBalanceAmount(aa);
		w.setWorkCode(dto.getWorkCode() != null ? dto.getWorkCode() : w.getWorkCode());
		w.setStatus(WorkStatuses.AA_APPROVED);

		if (aaLetterFile != null && !aaLetterFile.isEmpty()) {
			String uploadedUrl = fileStorageService.saveFile(aaLetterFile, "aa-letters");
			w.setAdminApprovedletterUrl(uploadedUrl);
		} else if (dto.getAaLetterUrl() != null && !dto.getAaLetterUrl().isBlank()) {
			w.setAdminApprovedletterUrl(dto.getAaLetterUrl());
		}

		// === Budget impact (AA is the final sanction for allocation/consumption) ===
		Schemes scheme = w.getScheme();
		if (scheme == null)
			throw new RuntimeException("Work has no scheme");
		var plan = scheme.getPlanType();

		// >>> CHANGED: use FY from WORK, not from SCHEME
		FinancialYear fy = w.getFinancialYear();
		if (fy == null)
			throw new RuntimeException("Work has no financial year");

		if (plan == PlanType.MLA) {
			if (w.getRecommendedByMla() == null)
				throw new RuntimeException("MLA budget adjustment requires MLA recommender");
			List<MLABudget> rows = mlaBudgetRepo.findByMla_IdAndFinancialYear_Id(w.getRecommendedByMla().getId(),
					fy.getId());
			MLABudget mb = firstOrThrow(rows, "MLA Budget row not found");
			if (nz(mb.getUtilizedLimit()) + aa > nz(mb.getAllocatedLimit())) {
				throw new RuntimeException("AA exceeds MLA available budget");
			}
			mb.setUtilizedLimit(nz(mb.getUtilizedLimit()) + aa);
			mlaBudgetRepo.save(mb);

		} else if (plan == PlanType.MLC) {
			if (w.getRecommendedByMlc() == null)
				throw new RuntimeException("MLC budget adjustment requires MLC recommender");
			List<MLCBudget> rows = mlcBudgetRepo.findByMlc_IdAndFinancialYear_Id(w.getRecommendedByMlc().getId(),
					fy.getId());
			MLCBudget mb = firstOrThrow(rows, "MLC Budget row not found");
			if (nz(mb.getUtilizedLimit()) + aa > nz(mb.getAllocatedLimit())) {
				throw new RuntimeException("AA exceeds MLC available budget");
			}
			mb.setUtilizedLimit(nz(mb.getUtilizedLimit()) + aa);
			mlcBudgetRepo.save(mb);

		} else if (plan == PlanType.HADP) {
			// Adjust taluka-level HADP budget (by Work's FY)
			Long dId = w.getDistrict() != null ? w.getDistrict().getId() : null;
			Long tId = (w.getHadp() != null && w.getHadp().getTaluka() != null) ? w.getHadp().getTaluka().getId()
					: null;

			List<HADPBudget> all = hadpBudgetRepo.findAll();
			HADPBudget budget = all.stream()
					.filter(b -> b.getTaluka() != null && Objects.equals(b.getTaluka().getId(), tId))
					.filter(b -> b.getFinancialYear() != null
							&& Objects.equals(b.getFinancialYear().getId(), fy.getId()))
					.findFirst().orElseThrow(() -> new RuntimeException("HADP Taluka budget row not found"));

			if (nz(budget.getUtilizedLimit()) + aa > nz(budget.getAllocatedLimit())) {
				throw new RuntimeException("AA exceeds HADP taluka available budget");
			}
			budget.setUtilizedLimit(nz(budget.getUtilizedLimit()) + aa);
			budget.setRemainingLimit(nz(budget.getAllocatedLimit()) - nz(budget.getUtilizedLimit()));
			hadpBudgetRepo.save(budget);

		} else if (plan == PlanType.DAP) {
			if (scheme.getSchemeType() == null)
				throw new RuntimeException("DAP requires a DemandCode on scheme");
			DemandCode dc = scheme.getSchemeType();
			Long dId = w.getDistrict() != null ? w.getDistrict().getId() : null;
			if (dId == null)
				throw new RuntimeException("Work district is required for DAP");

			var dla = dlaRepo
					.findByDistrict_IdAndPlanTypeAndSchemeType_IdAndFinancialYear_Id(dId, PlanType.DAP, dc.getId(),
							fy.getId())
					.orElseThrow(() -> new RuntimeException("District Limit Allocation not found for DAP"));

			if (nz(dla.getUtilizedLimit()) + aa > nz(dla.getAllocatedLimit())) {
				throw new RuntimeException("AA exceeds DAP available district allocation");
			}
			dla.setUtilizedLimit(nz(dla.getUtilizedLimit()) + aa);
			dla.setRemainingLimit(nz(dla.getAllocatedLimit()) - nz(dla.getUtilizedLimit()));
			dlaRepo.save(dla);

			var sb = schemeBudgetRepo.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(dId, scheme.getId(), fy.getId())
					.orElse(null);
			if (sb != null) {
				if (nz(sb.getUtilizedLimit()) + aa > nz(sb.getAllocatedLimit())) {
					throw new RuntimeException("AA exceeds scheme budget");
				}
				sb.setUtilizedLimit(nz(sb.getUtilizedLimit()) + aa);
				sb.setRemainingLimit(nz(sb.getAllocatedLimit()) - nz(sb.getUtilizedLimit()));
				schemeBudgetRepo.save(sb);
			}
		}

		return workRepo.save(w);
	}

	// -------------------- Role-aware list (basic) --------------------
	@Override
	public List<Work> listWorksForUser(String username, WorkListFilter f) {
		User u = user(username);

		if (u.getRole().name().startsWith("STATE")) {
			return applyFilters(workRepo.findAll(), f);
		}

		switch (u.getRole()) {
		case DISTRICT_COLLECTOR, DISTRICT_DPO, DISTRICT_ADPO, DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER -> {
			Long dId = u.getDistrict() != null ? u.getDistrict().getId() : null;
			if (dId == null)
				throw new RuntimeException("User has no district");
			return applyFilters(workRepo.findByDistrict_Id(dId), f);
		}
		case IA_ADMIN -> {
			return applyFilters(workRepo.findByImplementingAgency_Id(u.getId()), f);
		}
		case MLA, MLA_REP -> {
			MLA mla = (u.getRole() == Roles.MLA) ? mlaRepo.findByUser_Id(u.getId()).orElse(null) : null;
			if (mla == null && u.getRole() == Roles.MLA) {
				throw new RuntimeException("MLA record not found");
			}
			List<Work> list = (mla != null) ? workRepo.findByRecommendedByMla_Id(mla.getId()) : List.of();
			return applyFilters(list, f);
		}
		case MLC -> {
			MLC mlc = mlcRepo.findByUser_Id(u.getId()).orElseThrow(() -> new RuntimeException("MLC record not found"));
			return applyFilters(workRepo.findByRecommendedByMlc_Id(mlc.getId()), f);
		}
		default -> throw new RuntimeException("Unsupported role for listing works");
		}
	}

	private List<Work> applyFilters(List<Work> works, WorkListFilter f) {
		return works.stream()
				.filter(w -> f.getDistrictId() == null
						|| (w.getDistrict() != null && Objects.equals(w.getDistrict().getId(), f.getDistrictId())))
				.filter(w -> f.getConstituencyId() == null || (w.getConstituency() != null
						&& Objects.equals(w.getConstituency().getId(), f.getConstituencyId())))
				.filter(w -> f.getPlanType() == null
						|| (w.getScheme() != null && w.getScheme().getPlanType() == f.getPlanType()))
				.filter(w -> f.getFinancialYearId() == null
						|| (w.getScheme() != null && w.getScheme().getFinancialYear() != null
								&& Objects.equals(w.getScheme().getFinancialYear().getId(), f.getFinancialYearId())))
				.filter(w -> {
					if (f.getSearch() == null || f.getSearch().isBlank())
						return true;
					String s = f.getSearch().toLowerCase(Locale.ROOT);
					String name = w.getWorkName() != null ? w.getWorkName().toLowerCase(Locale.ROOT) : "";
					String code = w.getWorkCode() != null ? w.getWorkCode().toLowerCase(Locale.ROOT) : "";
					return name.contains(s) || code.contains(s);
				}).collect(Collectors.toList());
	}

	// -------------------- Status listing & details --------------------
	@Override
	@Transactional(readOnly = true)
	public List<WorkDetailsWithStatusDto> getWorksByStatus(String username, WorkStatuses status, Long districtId,
			Long mlaId, Long mlcId) {
		User u = user(username);
		List<Work> works = getWorksBasedOnUserRole(u, districtId, mlaId, mlcId);
		if (status != null) {
			works = works.stream().filter(w -> w.getStatus() == status).collect(Collectors.toList());
		}
		return works.stream().map(this::buildWorkDetailsWithStatusDto).collect(Collectors.toList());
	}

	@Transactional(readOnly = true)
	public List<WorkDetailsWithStatusDto> browseWorks(String username, PlanType planType, WorkStatuses status,
			Long districtId, Long iaUserId, Long vendorId, String mlaName, String mlcName, String q, Integer page,
			Integer size, String sortBy, String sortDir) {
		User u = user(username);
		List<Work> base = switch (u.getRole()) {
		case STATE_ADMIN, STATE_CHECKER, STATE_MAKER -> workRepo.findAll();
		case DISTRICT_COLLECTOR, DISTRICT_DPO, DISTRICT_ADPO, DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER -> {
			if (u.getDistrict() == null)
				yield List.of();
			yield workRepo.findByDistrict_Id(u.getDistrict().getId());
		}
		case IA_ADMIN -> workRepo.findByImplementingAgency_Id(u.getId());
		case MLA, MLA_REP -> {
			MLA mla = mlaRepo.findByUser_Id(u.getId()).orElse(null);
			yield (mla != null) ? workRepo.findByRecommendedByMla_Id(mla.getId()) : List.of();
		}
		case MLC -> {
			MLC mlc = mlcRepo.findByUser_Id(u.getId()).orElse(null);
			yield (mlc != null) ? workRepo.findByRecommendedByMlc_Id(mlc.getId()) : List.of();
		}
		default -> List.of();
		};

		List<Work> filtered = base.stream()
				.filter(w -> planType == null || (w.getScheme() != null && w.getScheme().getPlanType() == planType))
				.filter(w -> status == null || w.getStatus() == status)
				.filter(w -> districtId == null
						|| (w.getDistrict() != null && Objects.equals(w.getDistrict().getId(), districtId)))
				.filter(w -> iaUserId == null || (w.getImplementingAgency() != null
						&& Objects.equals(w.getImplementingAgency().getId(), iaUserId)))
				.filter(w -> vendorId == null
						|| (w.getVendor() != null && Objects.equals(w.getVendor().getId(), vendorId)))
				.filter(w -> {
					if (mlaName == null || mlaName.isBlank())
						return true;
					return w.getRecommendedByMla() != null && w.getRecommendedByMla().getMlaName() != null
							&& w.getRecommendedByMla().getMlaName().toLowerCase(Locale.ROOT)
									.contains(mlaName.toLowerCase(Locale.ROOT));
				}).filter(w -> {
					if (mlcName == null || mlcName.isBlank())
						return true;
					return w.getRecommendedByMlc() != null && w.getRecommendedByMlc().getMlcName() != null
							&& w.getRecommendedByMlc().getMlcName().toLowerCase(Locale.ROOT)
									.contains(mlcName.toLowerCase(Locale.ROOT));
				}).filter(w -> {
					if (q == null || q.isBlank())
						return true;
					String s = q.toLowerCase(Locale.ROOT);
					String name = w.getWorkName() != null ? w.getWorkName().toLowerCase(Locale.ROOT) : "";
					String code = w.getWorkCode() != null ? w.getWorkCode().toLowerCase(Locale.ROOT) : "";
					String dist = (w.getDistrict() != null && w.getDistrict().getDistrictName() != null)
							? w.getDistrict().getDistrictName().toLowerCase(Locale.ROOT)
							: "";
					String schemeName = (w.getScheme() != null && w.getScheme().getSchemeName() != null)
							? w.getScheme().getSchemeName().toLowerCase(Locale.ROOT)
							: "";
					String mlaS = (w.getRecommendedByMla() != null && w.getRecommendedByMla().getMlaName() != null)
							? w.getRecommendedByMla().getMlaName().toLowerCase(Locale.ROOT)
							: "";
					String mlcS = (w.getRecommendedByMlc() != null && w.getRecommendedByMlc().getMlcName() != null)
							? w.getRecommendedByMlc().getMlcName().toLowerCase(Locale.ROOT)
							: "";
					return name.contains(s) || code.contains(s) || dist.contains(s) || schemeName.contains(s)
							|| mlaS.contains(s) || mlcS.contains(s);
				}).collect(Collectors.toList());

		// sort (when amount requested, sort by recommendedAmount)
		Comparator<Work> cmp = Comparator.comparing(Work::getCreatedAt,
				Comparator.nullsLast(Comparator.naturalOrder()));
		if ("amount".equalsIgnoreCase(sortBy)) {
			cmp = Comparator.comparing(Work::getRecommendedAmount, Comparator.nullsLast(Comparator.naturalOrder()));
		} else if ("status".equalsIgnoreCase(sortBy)) {
			cmp = Comparator.comparing(w -> w.getStatus() != null ? w.getStatus().name() : "",
					Comparator.nullsLast(Comparator.naturalOrder()));
		}
		if ("desc".equalsIgnoreCase(sortDir))
			cmp = cmp.reversed();
		filtered.sort(cmp);

		// paginate
		if (page != null && size != null && page >= 0 && size > 0) {
			int from = Math.min(page * size, filtered.size());
			int to = Math.min(from + size, filtered.size());
			filtered = filtered.subList(from, to);
		}

		return filtered.stream().map(this::buildWorkDetailsWithStatusDto).collect(Collectors.toList());
	}

	private List<Work> getWorksBasedOnUserRole(User user, Long districtId, Long mlaId, Long mlcId) {
		List<Work> works;

		if (isStateUser(user.getRole())) {
			works = workRepo.findAll();
		} else if (isDistrictUser(user.getRole())) {
			if (user.getDistrict() == null) {
				works = List.of();
			} else {
				works = workRepo.findByDistrict_Id(user.getDistrict().getId());
			}
		} else if (user.getRole() == Roles.IA_ADMIN) {
			works = workRepo.findByImplementingAgency_Id(user.getId());
		} else if (user.getRole() == Roles.MLA || user.getRole() == Roles.MLA_REP) {
			MLA mla = mlaRepo.findByUser_Id(user.getId()).orElse(null);
			works = (mla != null) ? workRepo.findByRecommendedByMla_Id(mla.getId()) : List.of();
		} else if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepo.findByUser_Id(user.getId()).orElse(null);
			works = (mlc != null) ? workRepo.findByRecommendedByMlc_Id(mlc.getId()) : List.of();
		} else {
			works = List.of();
		}

		if (districtId != null) {
			works = works.stream()
					.filter(work -> work.getDistrict() != null && work.getDistrict().getId().equals(districtId))
					.collect(Collectors.toList());
		}
		if (mlaId != null) {
			works = works.stream().filter(
					work -> work.getRecommendedByMla() != null && work.getRecommendedByMla().getId().equals(mlaId))
					.collect(Collectors.toList());
		}
		if (mlcId != null) {
			works = works.stream().filter(
					work -> work.getRecommendedByMlc() != null && work.getRecommendedByMlc().getId().equals(mlcId))
					.collect(Collectors.toList());
		}

		return works;
	}

	private WorkDetailsWithStatusDto buildWorkDetailsWithStatusDto(Work w) {
		List<FundDemand> fundDemands = fundDemandRepo.findByWork_Id(w.getId());

		double totalFundDisbursed = fundDemands.stream().filter(d -> d.getStatus() == DemandStatuses.TRANSFERRED)
				.mapToDouble(d -> d.getNetPayable() != null ? d.getNetPayable() : 0.0).sum();
		double totalFundDemanded = fundDemands.stream().mapToDouble(d -> d.getAmount() != null ? d.getAmount() : 0.0)
				.sum();

		double utilizationProgressPercentage = 0.0;
		if (w.getAdminApprovedAmount() != null && w.getAdminApprovedAmount() > 0) {
			utilizationProgressPercentage = (totalFundDisbursed / w.getAdminApprovedAmount()) * 100;
		}

		int totalDemandsCount = fundDemands.size();
		int approvedDemandsCount = (int) fundDemands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.APPROVED_BY_STATE
						|| d.getStatus() == DemandStatuses.APPROVED_BY_DISTRICT)
				.count();
		int pendingDemandsCount = (int) fundDemands.stream().filter(d -> d.getStatus() == DemandStatuses.PENDING)
				.count();
		int rejectedDemandsCount = (int) fundDemands.stream()
				.filter(d -> d.getStatus() == DemandStatuses.REJECTED_BY_STATE
						|| d.getStatus() == DemandStatuses.REJECTED_BY_DISTRICT)
				.count();

		return WorkDetailsWithStatusDto.builder().workId(w.getId()).workName(w.getWorkName()).workCode(w.getWorkCode())
				.description(w.getDescription()).remark(w.getRemark()).status(w.getStatus())
				.workStartDate(w.getWorkStartDate()).workEndDate(w.getWorkEndDate()).sanctionDate(w.getSanctionDate())
				.createdAt(w.getCreatedAt()).createdBy(w.getCreatedBy())
				// NEW
				.recommendedAmount(w.getRecommendedAmount()).districtApprovedAmount(w.getDistrictApprovedAmount())
				// existing
				.grossAmount(w.getGrossAmount()).adminApprovedAmount(w.getAdminApprovedAmount())
				.balanceAmount(w.getBalanceAmount()).workPortionTax(w.getWorkPortionTax())
				.workPortionAmount(w.getWorkPortionAmount())
				.schemeId(w.getScheme() != null ? w.getScheme().getId() : null)
				.schemeName(w.getScheme() != null ? w.getScheme().getSchemeName() : null)
				.planType(w.getScheme() != null && w.getScheme().getPlanType() != null
						? w.getScheme().getPlanType().name()
						: null)
				.financialYearId(w.getFinancialYear() != null ? w.getFinancialYear().getId() : null)
				.financialYear(w.getFinancialYear() != null ? w.getFinancialYear().getFinacialYear() : null)
				.districtId(w.getDistrict() != null ? w.getDistrict().getId() : null)
				.districtName(w.getDistrict() != null ? w.getDistrict().getDistrictName() : null)
				.constituencyId(w.getConstituency() != null ? w.getConstituency().getId() : null)
				.constituencyName(w.getConstituency() != null ? w.getConstituency().getConstituencyName() : null)
				.mlaId(w.getRecommendedByMla() != null ? w.getRecommendedByMla().getId() : null)
				.mlaName(w.getRecommendedByMla() != null ? w.getRecommendedByMla().getMlaName() : null)
				.mlaParty(w.getRecommendedByMla() != null ? w.getRecommendedByMla().getParty() : null)
				.mlcId(w.getRecommendedByMlc() != null ? w.getRecommendedByMlc().getId() : null)
				.mlcName(w.getRecommendedByMlc() != null ? w.getRecommendedByMlc().getMlcName() : null)
				.mlcCategory(w.getRecommendedByMlc() != null ? w.getRecommendedByMlc().getCategory() : null)
				.isNodalWork(w.getIsNodalWork())
				.implementingAgencyId(w.getImplementingAgency() != null ? w.getImplementingAgency().getId() : null)
				.implementingAgencyName(w.getImplementingAgency() != null ? w.getImplementingAgency().getFullname()
						: null)
				.implementingAgencyUsername(w
						.getImplementingAgency() != null ? w.getImplementingAgency().getUsername()
								: null)
				.implementingAgencyMobile(
						w.getImplementingAgency() != null ? (w.getImplementingAgency().getMobile() != null
								? w.getImplementingAgency().getMobile().toString()
								: null) : null)
				.vendor(w.getVendor() != null ? WorkDetailsWithStatusDto.VendorDetailsDto.builder()
						.vendorId(w.getVendor().getId()).vendorName(w.getVendor().getName())
						.contactPerson(w.getVendor().getContactPerson()).phone(w.getVendor().getPhone())
						.email(w.getVendor().getEmail()).address(w.getVendor().getAddress())
						.city(w.getVendor().getCity()).state(w.getVendor().getState())
						.pincode(w.getVendor().getPincode()).gstNumber(w.getVendor().getGstNumber())
						.panNumber(w.getVendor().getPanNumber()).type(w.getVendor().getType())
						.category(w.getVendor().getCategory()).status(w.getVendor().getStatus())
						.performanceRating(w.getVendor().getPerformanceRating())
						.registrationDate(w.getVendor().getRegistrationDate())
						.aadhaarVerified(w.getVendor().getAadhaarVerified()).gstVerified(w.getVendor().getGstVerified())
						.paymentEligible(w.getVendor().getPaymentEligible()).build() : null)
				.totalFundDisbursed(totalFundDisbursed).totalFundDemanded(totalFundDemanded)
				.utilizationProgressPercentage(utilizationProgressPercentage).totalDemandsCount(totalDemandsCount)
				.approvedDemandsCount(approvedDemandsCount).pendingDemandsCount(pendingDemandsCount)
				.rejectedDemandsCount(rejectedDemandsCount)
				.fundDemands(fundDemands.stream().map(this::buildWorkFundDemandSummaryDto).toList())
				.recommendationLetterUrl(w.getRecommendationLetterUrl())
				.adminApprovedLetterUrl(w.getAdminApprovedletterUrl()).workOrderLetterUrl(w.getWorkOrderLetterUrl())
				.build();
	}

	private WorkDetailsWithStatusDto.FundDemandSummaryDto buildWorkFundDemandSummaryDto(FundDemand demand) {
		FundTransfer transfer = fundTransferRepository.findByFundDemand_Id(demand.getId()).orElse(null);
		return WorkDetailsWithStatusDto.FundDemandSummaryDto.builder().demandId(demand.getId())
				.demandCode(demand.getDemandId()).amount(demand.getAmount()).netPayable(demand.getNetPayable())
				.demandDate(demand.getDemandDate())
				.status(demand.getStatus() != null ? demand.getStatus().name() : null).createdAt(demand.getCreatedAt())
				.createdBy(demand.getCreatedBy()).fundDisbursed(transfer != null ? transfer.getTransferAmount() : 0.0)
				.transferStatus(transfer != null ? "TRANSFERRED" : "PENDING").build();
	}

	// -------------------- District approvals: ADPO → DPO → Collector
	// --------------------
	private boolean canAdpoStageApprove(Roles role) {
		return role == Roles.DISTRICT_ADPO || role == Roles.DISTRICT_MAKER;
	}

	private boolean canDpoStageApprove(Roles role) {
		return role == Roles.DISTRICT_DPO || role == Roles.DISTRICT_CHECKER;
	}

	private boolean canCollectorStageApprove(Roles role) {
		return role == Roles.DISTRICT_COLLECTOR || role == Roles.DISTRICT_ADMIN;
	}

	@Override
	@Transactional
	public Work approveByAdpo(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl) {
		User approver = user(approverUsername);

		if (!canAdpoStageApprove(approver.getRole())) {
			throw new RuntimeException("Only DISTRICT_ADPO or DISTRICT_MAKER can approve at ADPO stage (found role="
					+ approver.getRole() + ")");
		}

		Work w = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));
		if (w.getStatus() != WorkStatuses.PROPOSED) {
			throw new RuntimeException("Work must be in PROPOSED status for ADPO approval");
		}
		if (approver.getDistrict() == null || w.getDistrict() == null
				|| !approver.getDistrict().getId().equals(w.getDistrict().getId())) {
			throw new RuntimeException("ADPO must belong to the same district as the work");
		}

		if ("APPROVED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.ADPO_APPROVED);
			// set/adjust district-approved amount (no gross/tax here)
			Double base = nz(w.getRecommendedAmount());
			Double adj = (dto.getAdjustedAmount() != null && dto.getAdjustedAmount() > 0) ? dto.getAdjustedAmount()
					: base;
			w.setDistrictApprovedAmount(adj);
		} else if ("REJECTED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.ADPO_REJECTED);
		} else {
			throw new RuntimeException("Invalid approval status. Must be APPROVED or REJECTED");
		}

		w.setRemark(dto.getRemarks());

		if (approvalLetterUrl != null && !approvalLetterUrl.isEmpty()) {
			w.setAdminApprovedletterUrl(approvalLetterUrl);
		}

		return workRepo.save(w);
	}

	@Override
	@Transactional
	public Work approveByDpo(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl) {
		User approver = user(approverUsername);

		if (!canDpoStageApprove(approver.getRole())) {
			throw new RuntimeException("Only DISTRICT_DPO or DISTRICT_CHECKER can approve at DPO stage (found role="
					+ approver.getRole() + ")");
		}

		Work w = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));

		if (w.getStatus() != WorkStatuses.ADPO_APPROVED) {
			throw new RuntimeException("Work must be in ADPO_APPROVED status for DPO/Checker approval");
		}

		if (approver.getDistrict() == null || w.getDistrict() == null
				|| !approver.getDistrict().getId().equals(w.getDistrict().getId())) {
			throw new RuntimeException("DPO/Checker must belong to the same district as the work");
		}

		if ("APPROVED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.DPO_APPROVED);
			Double base = (w.getDistrictApprovedAmount() != null && w.getDistrictApprovedAmount() > 0)
					? w.getDistrictApprovedAmount()
					: nz(w.getRecommendedAmount());
			Double adj = (dto.getAdjustedAmount() != null && dto.getAdjustedAmount() > 0) ? dto.getAdjustedAmount()
					: base;
			w.setDistrictApprovedAmount(adj);
		} else if ("REJECTED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.DPO_REJECTED);
		} else {
			throw new RuntimeException("Invalid approval status. Must be APPROVED or REJECTED");
		}

		w.setRemark(dto.getRemarks());

		if (approvalLetterUrl != null && !approvalLetterUrl.isEmpty()) {
			w.setAdminApprovedletterUrl(approvalLetterUrl);
		}

		return workRepo.save(w);
	}

	@Override
	@Transactional
	public Work approveByCollector(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl) {
		User approver = user(approverUsername);

		if (!canCollectorStageApprove(approver.getRole())) {
			throw new RuntimeException(
					"Only DISTRICT_COLLECTOR or DISTRICT_ADMIN can approve at Collector stage (found role="
							+ approver.getRole() + ")");
		}

		Work w = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));

		if (w.getStatus() != WorkStatuses.DPO_APPROVED) {
			throw new RuntimeException("Work must be in DPO_APPROVED status for Collector/Admin approval");
		}

		if (approver.getDistrict() == null || w.getDistrict() == null
				|| !approver.getDistrict().getId().equals(w.getDistrict().getId())) {
			throw new RuntimeException("Collector/Admin must belong to the same district as the work");
		}

		if ("APPROVED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.COLLECTOR_APPROVED);
			Double base = (w.getDistrictApprovedAmount() != null && w.getDistrictApprovedAmount() > 0)
					? w.getDistrictApprovedAmount()
					: nz(w.getRecommendedAmount());
			Double adj = (dto.getAdjustedAmount() != null && dto.getAdjustedAmount() > 0) ? dto.getAdjustedAmount()
					: base;
			w.setDistrictApprovedAmount(adj);
		} else if ("REJECTED".equalsIgnoreCase(dto.getApprovalStatus())) {
			w.setStatus(WorkStatuses.COLLECTOR_REJECTED);
		} else {
			throw new RuntimeException("Invalid approval status. Must be APPROVED or REJECTED");
		}

		w.setRemark(dto.getRemarks());

		if (dto.getImplementingAgencyUserId() != null) {
			User ia = userRepo.findById(dto.getImplementingAgencyUserId())
					.orElseThrow(() -> new RuntimeException("Implementing agency user not found"));
			if (ia.getRole() != Roles.IA_ADMIN) {
				throw new RuntimeException("Implementing agency must be IA_ADMIN");
			}
			if (ia.getDistrict() == null || w.getDistrict() == null
					|| !ia.getDistrict().getId().equals(w.getDistrict().getId())) {
				throw new RuntimeException("Implementing agency must belong to the same district as the work");
			}
			w.setImplementingAgency(ia);
		}

		if (approvalLetterUrl != null && !approvalLetterUrl.isEmpty()) {
			w.setAdminApprovedletterUrl(approvalLetterUrl);
		}

		return workRepo.save(w);
	}

	@Override
	@Transactional
	public Work rejectWork(WorkApprovalDto dto, String rejectorUsername, String rejectionLetterUrl) {
		User rejector = user(rejectorUsername);

		boolean canReject = switch (rejector.getRole()) {
		case DISTRICT_ADPO, DISTRICT_DPO, DISTRICT_COLLECTOR, DISTRICT_ADMIN -> true;
		default -> false;
		};
		if (!canReject)
			throw new RuntimeException("Only district roles can reject works");

		Work w = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));

		if (rejector.getDistrict() == null || w.getDistrict() == null
				|| !rejector.getDistrict().getId().equals(w.getDistrict().getId())) {
			throw new RuntimeException("Rejector must belong to the same district as the work");
		}

		w.setStatus(WorkStatuses.REJECTED);
		w.setRemark(dto.getRemarks());
		if (rejectionLetterUrl != null && !rejectionLetterUrl.isEmpty()) {
			w.setAdminApprovedletterUrl(rejectionLetterUrl);
		}

		return workRepo.save(w);
	}

}
