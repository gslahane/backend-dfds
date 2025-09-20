package com.lsit.dfds.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.AssignWorkRequestDto;
import com.lsit.dfds.dto.FundDemandDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.WorkDetailsDto;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
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
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.HADPBudgetRepository;
import com.lsit.dfds.repo.HADPRepository;
import com.lsit.dfds.repo.MLABudgetRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLCBudgetRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchemeWorkMasterServiceImpl implements SchemeWorkMasterService {

	private final UserRepository userRepository;
	private final SchemesRepository schemesRepository;
	private final WorkRepository workRepository;
	private final FileStorageService fileStorageService;
	private final TalukaRepository talukaRepository;
	private final MLARepository mlaRepository;
	private final MLABudgetRepository mlaBudgetRepository;
	private final MLCRepository mlcRepository;
	private final MLCBudgetRepository mlcBudgetRepository;
	private final HADPRepository hadpRepository;
	private final HADPBudgetRepository hadpBudgetRepository;
	private final FinancialYearRepository financialYearRepository;
	private final FundDemandRepository fundDemandRepository;
	private final DistrictRepository districtRepository;
	private final SchemeBudgetRepository schemeBudgetRepository;

	@Override
	public List<IdNameDto> getSchemesForDistrict(String username, String planType, Long districtId) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// ✅ Determine district
		District targetDistrict = (districtId != null)
				? districtRepository.findById(districtId).orElseThrow(() -> new RuntimeException("District not found"))
				: user.getDistrict();

		PlanType typeEnum = (planType != null && !planType.isEmpty()) ? PlanType.valueOf(planType) : null;

		List<Schemes> schemes = schemesRepository.findByDistrictAndOptionalPlanType(targetDistrict, typeEnum);

		// ✅ Return only ID & Name
		return schemes.stream().map(s -> new IdNameDto(s.getId(), s.getSchemeName())).toList();
	}

	private boolean isState(Roles r) {
		return r != null && r.name().startsWith("STATE");
	}

	private boolean isDistrict(Roles r) {
		if (r == null)
			return false;
		return switch (r) {
		case DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER, DISTRICT_ADPO, DISTRICT_DPO, DISTRICT_COLLECTOR -> true;
		default -> false;
		};
	}

	@Override
	public List<IdNameDto> getIAUsers(String requesterUsername, Long districtId) {
		User requester = userRepository.findByUsername(requesterUsername)
				.orElseThrow(() -> new RuntimeException("User not found: " + requesterUsername));

		Roles role = requester.getRole();
		boolean state = isState(role);
		boolean district = isDistrict(role);
		boolean ia = role == Roles.IA_ADMIN;
		boolean mla = (role == Roles.MLA || role == Roles.MLA_REP);
		boolean mlc = (role == Roles.MLC);

		// --- STATE: all (optionally filter by districtId) ---
		if (state) {
			List<User> iaUsers = (districtId == null) ? userRepository.findByRole(Roles.IA_ADMIN)
					: userRepository.findByDistrict_IdAndRole(districtId, Roles.IA_ADMIN);

			return iaUsers.stream().sorted(Comparator.comparing(u -> displayName(u).toLowerCase()))
					.map(u -> new IdNameDto(u.getId(), displayName(u))).toList();
		}

		// Resolve caller's home district (for simple roles)
		Long requesterDistrictId = (requester.getDistrict() != null) ? requester.getDistrict().getId() : null;

		// --- DISTRICT / IA / MLA: only their own district ---
		if (district || ia || mla) {
			Long scope = (districtId == null) ? requesterDistrictId : districtId;
			if (scope == null) {
				throw new RuntimeException("District mapping missing; provide districtId or fix user mapping");
			}
			if (districtId != null && !Objects.equals(districtId, requesterDistrictId)) {
				throw new RuntimeException("Forbidden: you can only view IA users in your own district");
			}

			List<User> iaUsers = userRepository.findByDistrict_IdAndRole(scope, Roles.IA_ADMIN);
			return iaUsers.stream().sorted(Comparator.comparing(u -> displayName(u).toLowerCase()))
					.map(u -> new IdNameDto(u.getId(), displayName(u))).toList();
		}

		// --- MLC: allow explicit districtId without scope validation; otherwise use
		// mapped/accessible districts
		if (mlc) {
			// If caller provided a districtId, allow direct access (no "MLC scope"
			// blockage)
			if (districtId != null) {
				List<User> iaUsers = userRepository.findByDistrict_IdAndRole(districtId, Roles.IA_ADMIN);
				return iaUsers.stream().sorted(Comparator.comparing(u -> displayName(u).toLowerCase()))
						.map(u -> new IdNameDto(u.getId(), displayName(u))).toList();
			}

			// No districtId provided → fallback to MLC’s mapped scope
			MLC mlcRow = mlcRepository.findByUser_Id(requester.getId())
					.orElseThrow(() -> new RuntimeException("MLC profile not found"));

			Set<Long> allowed = new java.util.HashSet<>();

			// 1) from User mapping
			if (requesterDistrictId != null) {
				allowed.add(requesterDistrictId);
			}
			// 2) from MLC profile’s own district
			if (mlcRow.getDistrict() != null && mlcRow.getDistrict().getId() != null) {
				allowed.add(mlcRow.getDistrict().getId());
			}
			// 3) from accessibleDistricts
			if (mlcRow.getAccessibleDistricts() != null) {
				for (District d : mlcRow.getAccessibleDistricts()) {
					if (d != null && d.getId() != null) {
						allowed.add(d.getId());
					}
				}
			}

			if (allowed.isEmpty()) {
				// Changed message: don’t block with “no scope”; suggest passing districtId
				throw new RuntimeException(
						"No mapped districts for MLC. Please pass districtId to fetch IA users for a specific district.");
			}

			List<User> iaUsers = userRepository.findByDistrict_IdInAndRole(new java.util.ArrayList<>(allowed),
					Roles.IA_ADMIN);
			return iaUsers.stream().sorted(Comparator.comparing(u -> displayName(u).toLowerCase()))
					.map(u -> new IdNameDto(u.getId(), displayName(u))).toList();
		}

		throw new RuntimeException("Role " + role + " is not allowed to list IA users");
	}

	private String displayName(User u) {
		if (u.getFullname() != null && !u.getFullname().isBlank())
			return u.getFullname();
		if (u.getDesignation() != null && !u.getDesignation().isBlank())
			return u.getDesignation();
		return u.getUsername();
	}

	@Override
	@Transactional
	public Work assignWork(AssignWorkRequestDto dto, String username) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// ✅ Validate Scheme
		if (dto.getSchemeId() == null) {
			throw new RuntimeException("Scheme ID is required");
		}
		Schemes scheme = schemesRepository.findById(dto.getSchemeId())
				.orElseThrow(() -> new RuntimeException("Scheme not found"));

		// ✅ Validate Financial Year
		if (dto.getFinancialYearId() == null) {
			throw new RuntimeException("Financial Year ID is required");
		}
		FinancialYear financialYear = financialYearRepository.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));

		// ✅ Validate PlanType from Request
		if (dto.getPlanType() == null) {
			throw new RuntimeException("Plan Type is required");
		}

		// ✅ Ensure Scheme PlanType matches Request PlanType
		if (!scheme.getPlanType().equals(dto.getPlanType())) {
			throw new RuntimeException("Scheme Plan Type does not match the requested Plan Type");
		}

		// ✅ Handle file upload safely
		String filePath = (dto.getAaLetterFile() != null && !dto.getAaLetterFile().isEmpty())
				? fileStorageService.saveFile(dto.getAaLetterFile(), "work_docs")
				: null;

		Work work = new Work();
		work.setScheme(scheme);
		work.setWorkName(dto.getWorkTitle());
		work.setAdminApprovedAmount(dto.getAaAmount());
		work.setAdminApprovedletterUrl(filePath);
		work.setCreatedBy(username);
		// financialYear is a FinancialYear entity
		work.setFinancialYear(financialYear); // ✅

		// ✅ Assign Implementing Agency (IA User)
		if (dto.getIaUserId() != null) {
			User iaUser = userRepository.findById(dto.getIaUserId())
					.orElseThrow(() -> new RuntimeException("Implementing Agency not found"));
			work.setVendor(null); // If vendor selection is separate
			work.setDistrict(iaUser.getDistrict());
		}

		// ✅ Plan Type Specific Logic
		switch (dto.getPlanType()) {
		case DAP -> {
			District district = user.getDistrict();
			if (district == null) {
				throw new RuntimeException("Logged-in user is not mapped to any district");
			}
			updateSchemeBudgetForDistrict(district, scheme, dto.getAaAmount());
			work.setDistrict(district);
		}

		case MLA -> {
			if (dto.getMlaId() == null) {
				throw new RuntimeException("MLA ID is required for MLA Plan Type");
			}
			MLA mla = mlaRepository.findById(dto.getMlaId()).orElseThrow(() -> new RuntimeException("MLA not found"));
			updateMLABudget(mla, scheme, dto.getAaAmount());
			work.setDistrict(mla.getConstituency().getDistrict());
			work.setConstituency(mla.getConstituency());
			work.setRecommendedByMla(mla);
		}

		case MLC -> {
			if (dto.getMlcId() == null || dto.getDistrictId() == null) {
				throw new RuntimeException("MLC ID and District ID are required for MLC Plan Type");
			}
			MLC mlc = mlcRepository.findById(dto.getMlcId()).orElseThrow(() -> new RuntimeException("MLC not found"));
			District targetDistrict = districtRepository.findById(dto.getDistrictId())
					.orElseThrow(() -> new RuntimeException("District not found"));

			boolean isNodal = mlc.getAccessibleDistricts().stream()
					.anyMatch(d -> d.getId().equals(targetDistrict.getId()));

			work.setIsNodalWork(isNodal);
			work.setDistrict(targetDistrict);
			work.setRecommendedByMlc(mlc);
			work.setRemark(isNodal ? "Nodal Work" : "Non-Nodal Work");

			updateMLCBudget(mlc, scheme, dto.getAaAmount());
		}

		case HADP -> {
			if (dto.getTalukaId() == null) {
				throw new RuntimeException("Taluka ID is required for HADP Plan Type");
			}
			Taluka taluka = talukaRepository.findById(dto.getTalukaId())
					.orElseThrow(() -> new RuntimeException("Taluka not found"));
			updateHADPBudget(taluka, scheme, dto.getAaAmount());
			work.setDistrict(taluka.getDistrict());
		}
		}

		return workRepository.save(work);
	}

	private void updateSchemeBudgetForDistrict(District district, Schemes scheme, Double amount) {
		SchemeBudget budget = schemeBudgetRepository
				.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(district.getId(), scheme.getId(),
						scheme.getFinancialYear().getId())
				.orElseThrow(() -> new RuntimeException("Budget not allocated for this scheme"));

		if (budget.getRemainingLimit() < amount) {
			throw new RuntimeException("Insufficient budget for this work");
		}

		budget.setUtilizedLimit(budget.getUtilizedLimit() + amount);
		budget.setRemainingLimit(budget.getAllocatedLimit() - budget.getUtilizedLimit());
		schemeBudgetRepository.save(budget);
	}

	private void updateMLABudget(MLA mla, Schemes scheme, Double amount) {
		MLABudget budget = mlaBudgetRepository
				.findByMla_IdAndScheme_IdAndFinancialYear_Id(mla.getId(), scheme.getId(),
						scheme.getFinancialYear().getId())
				.orElseThrow(() -> new RuntimeException("MLA Budget not allocated for this scheme"));

		if (budget.getRemainingLimit() < amount) {
			throw new RuntimeException("Insufficient MLA budget");
		}

		budget.setUtilizedLimit(budget.getUtilizedLimit() + amount);
		mlaBudgetRepository.save(budget);
	}

	private void updateMLCBudget(MLC mlc, Schemes scheme, Double amount) {
		MLCBudget budget = mlcBudgetRepository
				.findByMlc_IdAndScheme_IdAndFinancialYear_Id(mlc.getId(), scheme.getId(),
						scheme.getFinancialYear().getId())
				.orElseThrow(() -> new RuntimeException("MLC Budget not allocated for this scheme"));

		if (budget.getRemainingLimit() < amount) {
			throw new RuntimeException("Insufficient MLC budget");
		}

		budget.setUtilizedLimit(budget.getUtilizedLimit() + amount);
		mlcBudgetRepository.save(budget);
	}

	private void updateHADPBudget(Taluka taluka, Schemes scheme, Double amount) {
		HADPBudget budget = hadpBudgetRepository
				.findByTaluka_IdAndScheme_IdAndFinancialYear_Id(taluka.getId(), scheme.getId(),
						scheme.getFinancialYear().getId())
				.orElseThrow(() -> new RuntimeException("HADP Budget not allocated for this scheme"));

		if (budget.getRemainingLimit() < amount) {
			throw new RuntimeException("Insufficient HADP budget");
		}

		budget.setUtilizedLimit(budget.getUtilizedLimit() + amount);
		budget.setRemainingLimit(budget.getAllocatedLimit() - budget.getUtilizedLimit());
		hadpBudgetRepository.save(budget);
	}

	@Override
	public List<WorkDetailsDto> getAssignedWorks(String username, String planType, Long schemeId, Long iaUserId,
			String search) {

		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		// ✅ Determine district restriction
		Long districtId = null;
		if (user.getRole() == Roles.DISTRICT_ADMIN || user.getRole() == Roles.DISTRICT_MAKER
				|| user.getRole() == Roles.DISTRICT_CHECKER || user.getRole() == Roles.IA_ADMIN) {
			districtId = user.getDistrict().getId(); // Restrict to their district
		}

		// ✅ If STATE_ADMIN / STATE_CHECKER / STATE_MAKER → do not restrict district
		boolean isStateRole = user.getRole() == Roles.STATE_ADMIN || user.getRole() == Roles.STATE_CHECKER
				|| user.getRole() == Roles.STATE_MAKER;

		PlanType typeEnum = (planType != null && !planType.isEmpty()) ? PlanType.valueOf(planType) : null;

		// ✅ Fetch works dynamically
		List<Work> works = workRepository.findWorksWithFilters(typeEnum, schemeId, isStateRole ? null : districtId, // For
				iaUserId, search);

		return works.stream().map(work -> {
			double allocatedBudget = 0.0;
			double utilizedBudget = 0.0;
			double remainingBudget = 0.0;

			if (work.getScheme() != null && work.getDistrict() != null) {
				var sbOpt = schemeBudgetRepository.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(
						work.getDistrict().getId(), work.getScheme().getId(),
						work.getScheme().getFinancialYear().getId());

				if (sbOpt.isPresent()) {
					var sb = sbOpt.get();
					allocatedBudget = sb.getAllocatedLimit();
					utilizedBudget = sb.getUtilizedLimit();
					remainingBudget = sb.getRemainingLimit();
				}
			}
			// ✅ Fetch fund demands
			List<FundDemandDto> fundDemands = fundDemandRepository.findByWork_Id(work.getId()).stream()
					.map(fd -> new FundDemandDto(fd.getId(), fd.getAmount(), fd.getNetPayable(), fd.getStatus().name(),
							fd.getDemandDate()))
					.toList();

			// ✅ Progress percentage
			double progress = (work.getAdminApprovedAmount() > 0)
					? (utilizedBudget / work.getAdminApprovedAmount()) * 100
					: 0.0;

			return new WorkDetailsDto(work.getId(), work.getWorkName(), work.getWorkCode(),
					work.getScheme() != null ? work.getScheme().getPlanType().name() : "-",
					work.getScheme() != null ? work.getScheme().getSchemeName() : "-",
					work.getDistrict() != null ? work.getDistrict().getDistrictName() : "-",
					work.getConstituency() != null ? work.getConstituency().getConstituencyName() : "-",
					work.getRecommendedByMla() != null ? work.getRecommendedByMla().getMlaName() : null,
					work.getRecommendedByMlc() != null ? work.getRecommendedByMlc().getMlcName() : null,
					work.getAdminApprovedAmount(), allocatedBudget, utilizedBudget, remainingBudget,
					allocatedBudget - utilizedBudget, progress, fundDemands);
		}).toList();
	}

	/*
	 * @Override public List<Schemes> getSchemesForDistrict(String username, String
	 * planType) { // TODO Auto-generated method stub return null; }
	 *
	 * @Override public List<User> getIAUsers(String username) { // TODO
	 * Auto-generated method stub return null; }
	 */
}
