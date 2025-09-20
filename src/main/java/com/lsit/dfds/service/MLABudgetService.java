package com.lsit.dfds.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.MLABudgetPostDto;
import com.lsit.dfds.dto.MLABudgetResponseDto;
import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLABudget;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.SchemesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MLABudgetService implements IMLABudgetService {

	private final MLABudgetRepository mlaBudgetRepo;
	private final MLARepository mlaRepo;
	private final SchemesRepository schemesRepo;
	private final FinancialYearRepository fyRepo;
	private final ConstituencyRepository constituencyRepository;
	private final MLATermRepository termRepo;
	private final FinancialYearParser fyParser;

	// private static final double MLA_ANNUAL_BASE_LIMIT = 5_00_00_000.0; // ₹5 Cr

	@Override
	@Transactional
	public MLABudgetResponseDto saveMLABudget(MLABudgetPostDto dto, String createdBy) {
		// Fetch financial year, MLA, and constituency
		FinancialYear fy = fyRepo.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));
		MLA mla = mlaRepo.findById(dto.getMlaId()).orElseThrow(() -> new RuntimeException("MLA not found"));
		Constituency constituency = constituencyRepository.findById(dto.getConstituencyId())
				.orElseThrow(() -> new RuntimeException("Constituency not found"));

		// State-provided base amount for the year (can be 0..N Cr)
		double stateBase = dto.getAllocatedLimit() == null ? 0.0 : dto.getAllocatedLimit();
		if (stateBase < 0) {
			throw new RuntimeException("Allocated limit cannot be negative");
		}

		// Upsert the FY row
		MLABudget budget = mlaBudgetRepo
				.findByMla_IdAndFinancialYear_IdAndConstituency_Id(mla.getId(), fy.getId(), constituency.getId())
				.orElse(new MLABudget());

		// Resolve carry-forward only within active term
		double carryIn = 0.0;
		var term = termRepo.findFirstByMla_IdAndActiveTrue(mla.getId()).orElse(null);
		if (term != null) {
			Integer cur = fyParser.startYearOf(fy);
			List<FinancialYear> all = fyRepo.findAll();
			FinancialYear prev = all.stream().filter(f -> fyParser.startYearOf(f) < cur)
					.max(Comparator.comparingInt(fyParser::startYearOf)).orElse(null);

			if (prev != null) {
				int ps = fyParser.startYearOf(prev);
				LocalDate prevStartDate = LocalDate.of(ps, 4, 1); // Assuming FY starts April 1
				LocalDate curStartDate = LocalDate.of(cur, 4, 1);
				LocalDate termStart = term.getTenureStartDate();
				LocalDate termEnd = term.getTenureEndDate();
				boolean prevInTerm = !prevStartDate.isBefore(termStart) && !prevStartDate.isAfter(termEnd);
				boolean curInTerm = !curStartDate.isBefore(termStart) && !curStartDate.isAfter(termEnd);
				if (prevInTerm && curInTerm) {
					var prevBudget = mlaBudgetRepo.findByMla_IdAndFinancialYear_IdAndConstituency_Id(mla.getId(),
							prev.getId(), constituency.getId()).orElse(null);
					if (prevBudget != null) {
						carryIn = Math.max(0.0, prevBudget.getRemainingLimit());
					}
				}
			}
		}

		// Compute new allocated = state base + carry-in
		double existingUtilized = budget.getUtilizedLimit() == null ? 0.0 : budget.getUtilizedLimit();
		double newAllocated = stateBase + carryIn;

		// Guard: cannot reduce allocated below what is already utilized
		if (newAllocated < existingUtilized) {
			throw new RuntimeException(
					"Allocated (" + newAllocated + ") cannot be less than already utilized (" + existingUtilized + ")");
		}

		// Persist updated budget
		budget.setFinancialYear(fy);
		budget.setMla(mla);
		budget.setConstituency(constituency);
		budget.setBaseAnnualLimit(stateBase); // State's allocation for this FY
		budget.setCarryForwardIn(carryIn);
		budget.setAllocatedLimit(newAllocated);
		budget.setUtilizedLimit(existingUtilized); // unchanged here
		budget.setStatus(Statuses.ACTIVE);
		budget.setRemarks(dto.getRemarks());
		budget.setCreatedBy(createdBy);

		// Optional scheme mapping (unchanged)

		budget.setScheme(null);

		// Save and return the updated MLABudgetResponseDto
		MLABudget savedBudget = mlaBudgetRepo.save(budget);

		// Map the saved budget to a DTO for the response
		MLABudgetResponseDto responseDto = new MLABudgetResponseDto();
		responseDto.setMlaId(savedBudget.getMla().getId());
		responseDto.setMlaName(savedBudget.getMla().getMlaName());
		responseDto.setConstituencyId(savedBudget.getConstituency().getId());
		responseDto.setConstituencyName(savedBudget.getConstituency().getConstituencyName());
		responseDto.setSchemeId(savedBudget.getScheme() != null ? savedBudget.getScheme().getId() : null);
		responseDto.setSchemeName(
				savedBudget.getScheme() != null ? savedBudget.getScheme().getSchemeName() : "No Scheme Assigned");
		responseDto.setFinancialYear(savedBudget.getFinancialYear().getFinacialYear());
		responseDto.setAllocatedLimit(savedBudget.getAllocatedLimit());
		responseDto.setUtilizedLimit(savedBudget.getUtilizedLimit());
		responseDto.setStatus(savedBudget.getStatus() != null ? savedBudget.getStatus().name() : "ACTIVE");

		// District handling
		if (savedBudget.getConstituency() != null && savedBudget.getConstituency().getDistrict() != null) {
			responseDto.setDistrict(savedBudget.getConstituency().getDistrict().getDistrictName());
		} else {
			responseDto.setDistrict("No District Assigned");
		}

		return responseDto;
	}

	@Override
	public List<MLABudgetResponseDto> getMLABudgetSummary(String username, Long financialYearId) {
		List<MLABudget> budgets = mlaBudgetRepo.findByFinancialYear_Id(financialYearId);
		return budgets.stream().map(b -> {
			var dto = new MLABudgetResponseDto();
			dto.setMlaId(b.getMla().getId());
			dto.setMlaName(b.getMla().getMlaName());
			dto.setConstituencyId(b.getConstituency().getId());
			dto.setConstituencyName(b.getConstituency().getConstituencyName());
			if (b.getScheme() != null) {
				dto.setSchemeId(b.getScheme().getId());
				dto.setSchemeName(b.getScheme().getSchemeName());
			} else {
				dto.setSchemeId(null);
				dto.setSchemeName("No Scheme Assigned");
			}
			dto.setFinancialYear(b.getFinancialYear().getFinacialYear());
			dto.setAllocatedLimit(b.getAllocatedLimit());
			dto.setUtilizedLimit(b.getUtilizedLimit());
			dto.setStatus(b.getStatus() != null ? b.getStatus().name() : "ACTIVE");
			return dto;
		}).toList();
	}

	// Fetch all MLA budget details with MLA information (name, constituency, etc.)
	@Override
	@Transactional
	public List<MLABudgetResponseDto> getAllMLABudgetDetails(String username, Long financialYearId) {
		// Fetch the list of budgets for the provided financial year ID
		List<MLABudget> budgets;

		if (financialYearId != null) {
			// Fetch the list of budgets for the provided financial year ID
			budgets = mlaBudgetRepo.findByFinancialYear_Id(financialYearId);
		} else {
			// Fetch all budgets if no financial year ID is provided
			budgets = mlaBudgetRepo.findAll();
		}

		// Map each budget entry to the MLABudgetResponseDto with the minimum required
		// details
		return budgets.stream().map(b -> {
			var dto = new MLABudgetResponseDto();

			// Set the minimal required fields
			dto.setMlaName(b.getMla().getMlaName()); // Type = MLA Name
			dto.setFinancialYear(b.getFinancialYear().getFinacialYear()); // Financial Year
			dto.setConstituencyName(b.getConstituency().getConstituencyName()); // Constituency
			dto.setAllocatedLimit(b.getAllocatedLimit()); // Allocated Limit
			dto.setStatus(b.getStatus() != null ? b.getStatus().name() : "ACTIVE"); // Status

			// Set District - assuming you have a District in your model
			if (b.getConstituency() != null && b.getConstituency().getDistrict() != null) {
				dto.setDistrict(b.getConstituency().getDistrict().getDistrictName()); // District
			} else {
				dto.setDistrict("No District Assigned"); // Default if no district is available
			}

			return dto;
		}).collect(Collectors.toList());
	}

}