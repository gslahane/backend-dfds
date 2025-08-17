package com.lsit.dfds.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final DistrictRepository districtRepository;
	private final ConstituencyRepository constituencyRepository;
	private final MLARepository mlaRepository;
	private final MLCRepository mlcRepository;
	private final TalukaRepository talukaRepository; // if needed for HADP scope
	private final PasswordEncoder passwordEncoder;
	private final BankAccountService bankAccountService;
	private final AuditService audit;

	@Override
	@Transactional
	public User registerUser(UserRegistrationDto dto, String createdBy) {
		// ---------- Creator & permissions ----------
		User creator = userRepository.findByUsername(createdBy)
				.orElseThrow(() -> new RuntimeException("Creator not found"));

		boolean creatorIsState = creator.getRole().name().startsWith("STATE");
		boolean creatorIsDist = creator.getRole().name().startsWith("DISTRICT");

		if (creatorIsState) {
			// STATE can create everything, including MLA/MLC/HADP
		} else if (creatorIsDist) {
			// District can create IA and district makers/checkers
			if (dto.getRole() != Roles.IA_ADMIN && dto.getRole() != Roles.DISTRICT_CHECKER
					&& dto.getRole() != Roles.DISTRICT_MAKER) {
				throw new RuntimeException("District can only create IA_ADMIN / DISTRICT_CHECKER / DISTRICT_MAKER");
			}
		} else {
			throw new RuntimeException("Unauthorized creator role");
		}

		// ---------- Basic validations ----------
		if (dto.getRole() == null)
			throw new RuntimeException("Role is required");
		if (dto.getUsername() == null || dto.getUsername().isBlank())
			throw new RuntimeException("Username is required");
		if (dto.getPassword() == null || dto.getPassword().isBlank())
			throw new RuntimeException("Password is required");
		if (userRepository.findByUsername(dto.getUsername()).isPresent())
			throw new RuntimeException("Username already taken");

		// ---------- Resolve district if required ----------
		District district = null;
		// For DISTRICT_* and IA_ADMIN, a district is mandatory
		if (dto.getRole().name().startsWith("DISTRICT") || dto.getRole() == Roles.IA_ADMIN) {
			Long did = dto.getDistrictId() != null ? dto.getDistrictId()
					: (creatorIsDist ? creator.getDistrict().getId() : null);
			if (did == null)
				throw new RuntimeException("districtId is required for DISTRICT_* and IA_ADMIN");
			district = districtRepository.findById(did).orElseThrow(() -> new RuntimeException("District not found"));
		} else if (creatorIsDist && dto.getDistrictId() == null) {
			// inherit creator's district if useful
			district = creator.getDistrict();
		} else if (dto.getDistrictId() != null) {
			district = districtRepository.findById(dto.getDistrictId())
					.orElseThrow(() -> new RuntimeException("District not found"));
		}

		// ---------- Create User ----------
		User user = new User();
		user.setFullname(dto.getFullname());
		user.setUsername(dto.getUsername());
		user.setEmail(dto.getEmail());
		user.setDesignation(dto.getDesignation());
		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user.setMobile(dto.getMobile());
		user.setAgencyCode(dto.getAgencyCode());
		user.setRole(dto.getRole());
		user.setStatus(Statuses.ACTIVE);
		user.setCreatedBy(createdBy);
		user.setDistrict(district);

		if (dto.getRole() == Roles.IA_ADMIN && creatorIsDist) {
			user.setSupervisor(creator); // IA under District
		}

		User saved = userRepository.save(user);

		// ---------- Role-specific domain linking/creation ----------
		switch (dto.getRole()) {
		case MLA -> {
			if (dto.getExistingMlaId() != null) {
				// link to existing MLA
				MLA mla = mlaRepository.findById(dto.getExistingMlaId())
						.orElseThrow(() -> new RuntimeException("Existing MLA not found"));
				mla.setUser(saved);
				mlaRepository.save(mla);
			} else {
				// create a new MLA record
				if (dto.getConstituencyId() == null || dto.getMlaName() == null || dto.getMlaName().isBlank())
					throw new RuntimeException("For new MLA, constituencyId and mlaName are required");

				Constituency cons = constituencyRepository.findById(dto.getConstituencyId())
						.orElseThrow(() -> new RuntimeException("Constituency not found"));

				MLA mla = new MLA();
				mla.setMlaName(dto.getMlaName());
				mla.setParty(dto.getMlaParty());
				mla.setContactNumber(dto.getMlaContactNumber());
				mla.setEmail(dto.getMlaEmail());
				mla.setTerm(dto.getMlaTerm());
				mla.setStatus(Statuses.ACTIVE);
				mla.setConstituency(cons);
				mla.setUser(saved);
				mlaRepository.save(mla);
			}
		}
		case MLA_REP -> {
			if (dto.getRepForMlaId() == null)
				throw new RuntimeException("repForMlaId is required for MLA_REP");
			MLA mla = mlaRepository.findById(dto.getRepForMlaId())
					.orElseThrow(() -> new RuntimeException("MLA not found"));
			// If you need a persistent relation "representativeOf", add such a field/table.
			// For now, user role is MLA_REP; use repForMlaId in business logic where
			// needed.
		}
		case MLC -> {
			if (dto.getExistingMlcId() != null) {
				MLC mlc = mlcRepository.findById(dto.getExistingMlcId())
						.orElseThrow(() -> new RuntimeException("Existing MLC not found"));
				mlc.setUser(saved);
				mlcRepository.save(mlc);
			} else {
				if (dto.getMlcName() == null || dto.getMlcName().isBlank())
					throw new RuntimeException("For new MLC, mlcName is required");

				MLC mlc = new MLC();
				mlc.setMlcName(dto.getMlcName());
				mlc.setCategory(dto.getMlcCategory());
				mlc.setContactNumber(dto.getMlcContactNumber());
				mlc.setEmail(dto.getMlcEmail());
				mlc.setTerm(dto.getMlcTerm());
				mlc.setRegion(dto.getMlcRegion());
				mlc.setStatus(Statuses.ACTIVE);
				// Optional access lists
				if (dto.getAccessibleDistrictIds() != null && !dto.getAccessibleDistrictIds().isEmpty()) {
					var dists = districtRepository.findAllById(dto.getAccessibleDistrictIds());
					mlc.setAccessibleDistricts(dists);
				}
				if (dto.getAccessibleConstituencyIds() != null && !dto.getAccessibleConstituencyIds().isEmpty()) {
					var cons = constituencyRepository.findAllById(dto.getAccessibleConstituencyIds());
					mlc.setAccessibleConstituencies(cons);
				}
				mlc.setUser(saved);
				mlcRepository.save(mlc);
			}
		}
		case HADP_ADMIN -> {
			// optional scoping to a taluka (if provided)
			if (dto.getTalukaId() != null) {
				var taluka = talukaRepository.findById(dto.getTalukaId())
						.orElseThrow(() -> new RuntimeException("Taluka not found"));
				// You can store user->taluka in a mapping table if needed. For now, district is
				// enough.
				if (saved.getDistrict() == null)
					saved.setDistrict(taluka.getDistrict());
			}
		}
		default -> {
			/* DISTRICT_*, IA_ADMIN, STATE_*, DIVISION_ADMIN handled by User only */ }
		}

		// ---------- Optional bank (null-safe) ----------
		if (dto.getBankAccountNumber() != null && dto.getBankIfsc() != null) {
			UserBankAccount acc = new UserBankAccount();
			acc.setUser(saved);
			acc.setAccountHolderName(saved.getFullname());
			acc.setAccountNumber(dto.getBankAccountNumber());
			acc.setIfsc(dto.getBankIfsc());
			acc.setBankName(dto.getBankName());
			acc.setBranch(dto.getBankBranch());
			acc.setAccountType(dto.getAccountType());
			bankAccountService.setPrimary(saved.getId(), acc, createdBy);
		}

		audit.log(createdBy, "REGISTER_USER", "User", saved.getId(),
				"{\"username\":\"" + saved.getUsername() + "\",\"role\":\"" + saved.getRole() + "\"}");

		return saved;
	}
}
