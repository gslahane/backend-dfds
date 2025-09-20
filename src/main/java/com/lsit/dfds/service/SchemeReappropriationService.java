// src/main/java/com/lsit/dfds/service/SchemeReappropriationService.java
package com.lsit.dfds.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.ReappropCreateDto;
import com.lsit.dfds.dto.ReappropDecisionDto;
import com.lsit.dfds.dto.ReappropViewDto;
import com.lsit.dfds.dto.SchemeWithBudgetDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.SchemeReappropriation;
import com.lsit.dfds.entity.SchemeReappropriationSource;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.ReappropriationStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemeReappropriationRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchemeReappropriationService {

	private final UserRepository userRepository;
	private final SchemeReappropriationRepository reappropRepo;
	private final SchemeBudgetRepository schemeBudgetRepo;
	private final FinancialYearRepository fyRepo;
	private final DemandCodeRepository dcRepo;
	private final SchemesRepository schemesRepo;

	// ---------- District: prepare options ----------
	public List<DemandCode> listDemandCodesForDistrict(String username, Long financialYearId, PlanType planType) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!isDistrictRole(me)) {
			throw new RuntimeException("Only District roles can fetch Demand Codes here");
		}

		Long districtId = Optional.ofNullable(me.getDistrict()).map(District::getId)
				.orElseThrow(() -> new RuntimeException("User not mapped to district"));

		return dcRepo.findForDistrictFyAndPlanType(districtId, financialYearId, planType);
	}

	public List<SchemeWithBudgetDto> listSchemesWithBudgetsForDistrict(String username, Long financialYearId,
			Long demandCodeId) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!isDistrictRole(me)) {
			throw new RuntimeException("Only District roles can fetch schemes here");
		}

		Long districtId = Optional.ofNullable(me.getDistrict()).map(District::getId)
				.orElseThrow(() -> new RuntimeException("User not mapped to district"));

		List<SchemeBudget> sbs = schemeBudgetRepo.findForDistrictFyAndDemandCode(districtId, financialYearId,
				demandCodeId);

		return sbs.stream().map(sb -> new SchemeWithBudgetDto(sb.getScheme().getId(), sb.getScheme().getSchemeName(),
				nz(sb.getAllocatedLimit()), nz(sb.getUtilizedLimit()), nz(sb.getRemainingLimit()))).toList();
	}

	// ---------- District: create request ----------
	@Transactional
	public ReappropViewDto createRequest(String username, ReappropCreateDto dto) {

		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!isDistrictRole(me)) {
			throw new RuntimeException("Only District roles can create reappropriation request");
		}

		Long districtId = Optional.ofNullable(me.getDistrict()).map(District::getId)
				.orElseThrow(() -> new RuntimeException("User not mapped to district"));

		FinancialYear fy = fyRepo.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));
		DemandCode dc = dcRepo.findById(dto.getDemandCodeId())
				.orElseThrow(() -> new RuntimeException("Demand Code not found"));

		Schemes target = schemesRepo.findById(dto.getTargetSchemeId())
				.orElseThrow(() -> new RuntimeException("Target scheme not found"));

		// enforce same Demand Code
		if (target.getSchemeType() == null || !target.getSchemeType().getId().equals(dc.getId())) {
			throw new RuntimeException("Target scheme is not under the provided Demand Code");
		}
		// enforce target budget exists and is district+FY scoped
		SchemeBudget targetBudget = schemeBudgetRepo
				.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(districtId, target.getId(), fy.getId())
				.orElseThrow(() -> new RuntimeException("Target scheme budget not found for district/FY"));

		// validate sources (same DC, existing budgets, remaining >= amount)
		if (dto.getSources() == null || dto.getSources().isEmpty()) {
			throw new RuntimeException("At least one source scheme is required");
		}

		double total = 0.0;
		List<SchemeReappropriationSource> sources = new ArrayList<>();
		for (var s : dto.getSources()) {
			Schemes srcScheme = schemesRepo.findById(s.getSchemeId())
					.orElseThrow(() -> new RuntimeException("Source scheme not found: " + s.getSchemeId()));

			if (srcScheme.getSchemeType() == null || !srcScheme.getSchemeType().getId().equals(dc.getId())) {
				throw new RuntimeException(
						"Source scheme " + srcScheme.getSchemeName() + " is not under the Demand Code");
			}
			SchemeBudget srcBudget = schemeBudgetRepo
					.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(districtId, srcScheme.getId(), fy.getId())
					.orElseThrow(() -> new RuntimeException(
							"Source scheme budget not found for district/FY: " + srcScheme.getSchemeName()));

			double amt = s.getAmount() == null ? 0.0 : s.getAmount();
			if (amt <= 0) {
				throw new RuntimeException("Transfer amount must be positive");
			}
			if (nz(srcBudget.getRemainingLimit()) < amt) {
				throw new RuntimeException("Insufficient remaining in source: " + srcScheme.getSchemeName());
			}

			var src = new SchemeReappropriationSource();
			src.setSourceScheme(srcScheme);
			src.setTransferAmount(amt);
			src.setSourcePreviousBalance(nz(srcBudget.getRemainingLimit()));
			// updated balance will be set after approval; for now keep as previous
			src.setSourceUpdatedBalance(nz(srcBudget.getRemainingLimit()));
			sources.add(src);
			total += amt;
		}

		// create request (PENDING)
		SchemeReappropriation req = new SchemeReappropriation();
		req.setDistrict(me.getDistrict());
		req.setFinancialYear(fy);
		req.setSchemeType(dc);
		req.setTargetScheme(target);
		req.setTotalTransferAmount(total);
		req.setReappropriationDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
		req.setStatus(ReappropriationStatuses.PENDING);
		req.setRemarks(dto.getRemarks());
		req.setCreatedBy(username);

		// connect children
		sources.forEach(s -> s.setReappropriation(req));
		req.setSourceSchemes(sources);

		SchemeReappropriation saved = reappropRepo.save(req);

		return toView(saved);
	}

	// ---------- State: approve/reject ----------
	@Transactional
	public ReappropViewDto decide(String username, ReappropDecisionDto dto) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!isStateRole(me)) {
			throw new RuntimeException("Only STATE roles can take a decision");
		}

		SchemeReappropriation req = reappropRepo.findById(dto.getReappropId())
				.orElseThrow(() -> new RuntimeException("Reappropriation request not found"));

		if (req.getStatus() != ReappropriationStatuses.PENDING) {
			throw new RuntimeException("Request is not in PENDING state");
		}

		if (dto.getDecision() == ReappropriationStatuses.REJECTED) {
			req.setStatus(ReappropriationStatuses.REJECTED);
			req.setRemarks(dto.getRemarks());
			return toView(reappropRepo.save(req));
		}

		if (dto.getDecision() != ReappropriationStatuses.APPROVED) {
			throw new RuntimeException("Unsupported decision");
		}

		// On APPROVAL → adjust SchemeBudgets (allocation shift)
		Long districtId = req.getDistrict().getId();
		Long fyId = req.getFinancialYear().getId();

		// target budget
		SchemeBudget targetBudget = schemeBudgetRepo
				.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(districtId, req.getTargetScheme().getId(), fyId)
				.orElseThrow(() -> new RuntimeException("Target scheme budget not found (approval)"));

		// verify sources again and apply
		double totalIn = 0.0;
		for (SchemeReappropriationSource src : req.getSourceSchemes()) {
			SchemeBudget sb = schemeBudgetRepo
					.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(districtId, src.getSourceScheme().getId(), fyId)
					.orElseThrow(() -> new RuntimeException(
							"Source scheme budget missing (approval): " + src.getSourceScheme().getSchemeName()));

			double amt = nz(src.getTransferAmount());
			// we reduce allocated, not utilized; ensure utilized <= new allocated
			double newAllocated = nz(sb.getAllocatedLimit()) - amt;
			if (newAllocated < nz(sb.getUtilizedLimit())) {
				throw new RuntimeException(
						"Cannot reduce allocation below utilized for source: " + src.getSourceScheme().getSchemeName());
			}
			sb.setAllocatedLimit(newAllocated);
			sb.setRemainingLimit(newAllocated - nz(sb.getUtilizedLimit()));
			schemeBudgetRepo.save(sb);

			src.setSourceUpdatedBalance(sb.getRemainingLimit());
			totalIn += amt;
		}

		// increase target allocation
		double newTargetAlloc = nz(targetBudget.getAllocatedLimit()) + totalIn;
		targetBudget.setAllocatedLimit(newTargetAlloc);
		targetBudget.setRemainingLimit(newTargetAlloc - nz(targetBudget.getUtilizedLimit()));
		schemeBudgetRepo.save(targetBudget);

		req.setStatus(ReappropriationStatuses.APPROVED);
		req.setRemarks(dto.getRemarks());
		return toView(reappropRepo.save(req));
	}

	// ---------- State/District: list/search ----------
	public List<ReappropViewDto> listForState(Long financialYearId, Long districtId, Long demandCodeId,
			ReappropriationStatuses status) {
		return reappropRepo.search(districtId, financialYearId, demandCodeId, status).stream().map(this::toView)
				.toList();
	}

	public List<ReappropViewDto> listForDistrict(String username, Long financialYearId, Long demandCodeId,
			ReappropriationStatuses status) {
		User me = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!isDistrictRole(me)) {
			throw new RuntimeException("Only District roles can see their reappropriations");
		}

		Long districtId = Optional.ofNullable(me.getDistrict()).map(District::getId)
				.orElseThrow(() -> new RuntimeException("User not mapped to district"));

		return reappropRepo.search(districtId, financialYearId, demandCodeId, status).stream().map(this::toView)
				.toList();
	}

	// ---------- helpers ----------
	private boolean isDistrictRole(User u) {
		return u.getRole() == Roles.DISTRICT_ADMIN || u.getRole() == Roles.DISTRICT_CHECKER
				|| u.getRole() == Roles.DISTRICT_MAKER;
	}

	private boolean isStateRole(User u) {
		return u.getRole() == Roles.STATE_ADMIN || u.getRole() == Roles.STATE_CHECKER
				|| u.getRole() == Roles.STATE_MAKER;
	}

	private static double nz(Double d) {
		return d == null ? 0.0 : d;
	}

	private ReappropViewDto toView(SchemeReappropriation r) {
		return new ReappropViewDto(r.getId(), r.getDistrict().getId(), r.getDistrict().getDistrictName(),
				r.getFinancialYear().getId(), r.getFinancialYear().getFinacialYear(), r.getSchemeType().getId(),
				r.getSchemeType().getSchemeType(), r.getTargetScheme().getId(), r.getTargetScheme().getSchemeName(),
				r.getTotalTransferAmount(), r.getStatus() != null ? r.getStatus().name() : "PENDING",
				r.getReappropriationDate(), r.getRemarks(),
				r.getSourceSchemes() == null ? List.of()
						: r.getSourceSchemes().stream()
								.map(s -> new ReappropViewDto.ReappropViewSourceItem(s.getSourceScheme().getId(),
										s.getSourceScheme().getSchemeName(), s.getTransferAmount()))
								.collect(Collectors.toList()));
	}
}
