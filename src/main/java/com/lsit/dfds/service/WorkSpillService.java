package com.lsit.dfds.service;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.SpillCreateDto;
import com.lsit.dfds.dto.SpillDecisionDto;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.entity.HADPBudget;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCBudget;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.entity.WorkSpillRequest;
import com.lsit.dfds.enums.SpillStatuses;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;
import com.lsit.dfds.repo.WorkSpillRequestRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkSpillService {

	private final WorkRepository workRepo;
	private final FinancialYearRepository fyRepo;
	private final SchemesRepository schemesRepo;
	private final WorkSpillRequestRepository spillRepo;
	private final UserRepository userRepo;

	private final SchemeBudgetRepository schemeBudgetRepo; // DAP
	private final MLABudgetRepository mlaBudgetRepo; // MLA
	private final MLCBudgetRepository mlcBudgetRepo; // MLC
	private final HADPBudgetRepository hadpBudgetRepo; // HADP

	// 1) District/IA raises request
	@Transactional
	public WorkSpillRequest createSpill(String username, SpillCreateDto dto) {
		User me = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// Only DISTRICT_* or IA_ADMIN can request
		if (!(me.getRole().name().startsWith("DISTRICT") || me.getRole().name().equals("IA_ADMIN"))) {
			throw new RuntimeException("Only District/IA can raise spill request");
		}

		Work work = workRepo.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));
		Schemes fromScheme = work.getScheme();
		if (fromScheme == null) {
			throw new RuntimeException("Work not mapped to scheme");
		}

		FinancialYear toFy = fyRepo.findById(dto.getToFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Target FY not found"));
		FinancialYear fromFy = fromScheme.getFinancialYear();
		if (fromFy.getId().equals(toFy.getId())) {
			throw new RuntimeException("Target FY must be different");
		}

		// Find the next-FY scheme with same code (or same logical identity)
		String schemeCode = fromScheme.getSchemeCode();
		Schemes toScheme = schemesRepo.findBySchemeCodeAndFinancialYear_Id(schemeCode, toFy.getId())
				.orElseThrow(() -> new RuntimeException("Next-FY scheme with same code not found"));

		// Demand code must match
		if (fromScheme.getSchemeType() == null || toScheme.getSchemeType() == null
				|| !fromScheme.getSchemeType().getId().equals(toScheme.getSchemeType().getId())) {
			throw new RuntimeException("Spill requires same Demand Code in target FY");
		}

		// Carry-forward amount = current remaining liability
		double carry = work.getBalanceAmount() == null ? 0.0 : work.getBalanceAmount();
		if (carry <= 0) {
			throw new RuntimeException("No liability to carry forward");
		}

		WorkSpillRequest req = new WorkSpillRequest();
		req.setWork(work);
		req.setFromScheme(fromScheme);
		req.setToScheme(toScheme);
		req.setFromFy(fromFy);
		req.setToFy(toFy);
		req.setPlanType(fromScheme.getPlanType());
		req.setDistrict(work.getDistrict());
		req.setDemandCode(fromScheme.getSchemeType());
		req.setCarryForwardAmount(carry);
		req.setStatus(SpillStatuses.PENDING);
		req.setRemarks(dto.getRemarks());
		req.setCreatedBy(username);

		return spillRepo.save(req);
	}

	// 2) STATE approves or rejects
	@Transactional
	public WorkSpillRequest decide(String username, SpillDecisionDto dto) {
		User me = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (!me.getRole().name().startsWith("STATE")) {
			throw new RuntimeException("Only State can approve/reject spill");
		}

		WorkSpillRequest r = spillRepo.findById(dto.getSpillRequestId())
				.orElseThrow(() -> new RuntimeException("Spill request not found"));

		if (r.getStatus() != SpillStatuses.PENDING) {
			throw new RuntimeException("Request already decided");
		}

		if (!dto.isApprove()) {
			r.setStatus(SpillStatuses.REJECTED);
			r.setRemarks(dto.getRemarks());
			r.setDecidedBy(username);
			r.setDecidedAt(java.time.LocalDateTime.now());
			return spillRepo.save(r);
		}

		// APPROVAL path
		// 2a) Move budget into next-FY bucket (increase allocated+remaining)
		double amt = r.getCarryForwardAmount();
		switch (r.getPlanType()) {
		case DAP -> {
			// SchemeBudget for district + scheme(toScheme) + toFy
			SchemeBudget sb = schemeBudgetRepo.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(r.getDistrict().getId(),
					r.getToScheme().getId(), r.getToFy().getId()).orElseGet(() -> {
						SchemeBudget b = new SchemeBudget();
						b.setDistrict(r.getDistrict());
						b.setScheme(r.getToScheme());
						b.setSchemeType(r.getDemandCode());
						b.setFinancialYear(r.getToFy());
						b.setAllocatedLimit(0.0);
						b.setUtilizedLimit(0.0);
						b.setRemainingLimit(0.0);
						b.setStatus(com.lsit.dfds.enums.Statuses.ACTIVE);
						b.setCreatedBy(username);
						return b;
					});
			sb.setAllocatedLimit((sb.getAllocatedLimit() == null ? 0.0 : sb.getAllocatedLimit()) + amt);
			sb.setRemainingLimit((sb.getRemainingLimit() == null ? 0.0 : sb.getRemainingLimit()) + amt);
			schemeBudgetRepo.save(sb);
		}
		case MLA -> {
			// Increase MLA budget in toFy
			MLA mla = r.getWork().getRecommendedByMla();
			if (mla == null) {
				throw new RuntimeException("Work not tagged to any MLA");
			}
			MLABudget b = mlaBudgetRepo
					.findByMla_IdAndFinancialYear_IdAndConstituency_Id(mla.getId(), r.getToFy().getId(),
							r.getWork().getConstituency() != null ? r.getWork().getConstituency().getId() : null)
					.orElseGet(() -> {
						MLABudget nb = new MLABudget();
						nb.setMla(mla);
						nb.setFinancialYear(r.getToFy());
						nb.setConstituency(r.getWork().getConstituency());
						nb.setAllocatedLimit(0.0);
						nb.setUtilizedLimit(0.0);
						nb.setStatus(com.lsit.dfds.enums.Statuses.ACTIVE);
						nb.setCreatedBy(username);
						return nb;
					});
			b.setAllocatedLimit(NZ(b.getAllocatedLimit()) + amt);
			// remaining = getter computed; if you persist remaining, update it here
			// similarly
			mlaBudgetRepo.save(b);
		}
		case MLC -> {
			MLC mlc = r.getWork().getRecommendedByMlc();
			if (mlc == null) {
				throw new RuntimeException("Work not tagged to any MLC");
			}
			MLCBudget b = mlcBudgetRepo.findByMlc_IdAndFinancialYear_IdAndScheme_Id(mlc.getId(), r.getToFy().getId(),
					r.getToScheme().getId()).orElseGet(() -> {
						MLCBudget nb = new MLCBudget();
						nb.setMlc(mlc);
						nb.setFinancialYear(r.getToFy());
						nb.setScheme(r.getToScheme());
						nb.setAllocatedLimit(0.0);
						nb.setUtilizedLimit(0.0);
						nb.setStatus(com.lsit.dfds.enums.Statuses.ACTIVE);
						nb.setCreatedBy(username);
						return nb;
					});
			b.setAllocatedLimit(NZ(b.getAllocatedLimit()) + amt);
			mlcBudgetRepo.save(b);
		}
		case HADP -> {
			Taluka taluka = r.getWork().getHadp() != null ? r.getWork().getHadp().getTaluka() : null;
			if (taluka == null) {
				throw new RuntimeException("HADP spill requires taluka context");
			}
			HADP hadp = r.getWork().getHadp();
			if (hadp == null) {
				throw new RuntimeException("Work not linked to HADP");
			}
			HADPBudget b = hadpBudgetRepo
					.findByHadp_IdAndTaluka_IdAndFinancialYear_Id(hadp.getId(), taluka.getId(), r.getToFy().getId())
					.orElseGet(() -> {
						HADPBudget nb = new HADPBudget();
						nb.setHadp(hadp);
						nb.setScheme(r.getToScheme());
						// Set district from taluka

						nb.setTaluka(taluka);
						nb.setFinancialYear(r.getToFy());
						nb.setAllocatedLimit(0.0);
						nb.setUtilizedLimit(0.0);
						nb.setRemainingLimit(0.0);
						nb.setStatus(com.lsit.dfds.enums.Statuses.ACTIVE);
						nb.setCreatedBy(username);
						return nb;
					});
			b.setAllocatedLimit(NZ(b.getAllocatedLimit()) + amt);
			b.setRemainingLimit(NZ(b.getRemainingLimit()) + amt);
			hadpBudgetRepo.save(b);
		}
		}

		// 2b) Re-point work to next-FY scheme (so future FundDemands go into toFy)
		Work w = r.getWork();
		w.setScheme(r.getToScheme());
		workRepo.save(w);

		// 2c) Mark request approved
		r.setStatus(SpillStatuses.APPROVED);
		r.setRemarks(dto.getRemarks());
		r.setDecidedBy(username);
		r.setDecidedAt(java.time.LocalDateTime.now());
		return spillRepo.save(r);
	}

	private static double NZ(Double d) {
		return d == null ? 0.0 : d;
	}
}