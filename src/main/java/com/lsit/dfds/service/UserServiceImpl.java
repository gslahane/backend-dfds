package com.lsit.dfds.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.UserRegistrationDto;
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

//com.lsit.dfds.service.UserServiceImpl
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepo;
	private final DistrictRepository districtRepo;
	private final ConstituencyRepository constituencyRepo;
	private final MLARepository mlaRepo;
	private final MLCRepository mlcRepo;
	private final TalukaRepository talukaRepo;
	private final PasswordEncoder passwordEncoder;
	private final BankAccountService bankAccountService; // you already created this helper
	private final AuditService audit;

	private static boolean isState(Roles r) {
		return r != null && r.name().startsWith("STATE");
	}

	private static boolean isDistrictAny(Roles r) {
		return r != null && r.name().startsWith("DISTRICT");
	}

	@Override
	@Transactional
	public User registerDistrictUser(UserRegistrationDto dto, String createdBy) {
		// Force role group
		if (dto.getRole() == null)
			throw new RuntimeException("role is required");
		var allowed = java.util.Set.of(Roles.DISTRICT_COLLECTOR, Roles.DISTRICT_DPO, Roles.DISTRICT_ADPO,
				Roles.DISTRICT_ADMIN, Roles.DISTRICT_CHECKER, Roles.DISTRICT_MAKER);
		if (!allowed.contains(dto.getRole())) {
			throw new RuntimeException("Invalid role for /district-staff registration");
		}
		return registerUser(dto, createdBy);
	}

	@Override
	@Transactional
	public User registerUser(UserRegistrationDto dto, String createdBy) {
		// creator
		User creator = userRepo.findByUsername(createdBy).orElseThrow(() -> new RuntimeException("Creator not found"));
		Roles cr = creator.getRole();

		// permissions
		if (isState(cr)) {
			// STATE can create any role
		} else if (isDistrictAny(cr)) {
			// District can create district staff + IA admins
			var allowed = java.util.Set.of(Roles.DISTRICT_COLLECTOR, Roles.DISTRICT_DPO, Roles.DISTRICT_ADPO,
					Roles.DISTRICT_ADMIN, Roles.DISTRICT_CHECKER, Roles.DISTRICT_MAKER, Roles.IA_ADMIN);
			if (!allowed.contains(dto.getRole())) {
				throw new RuntimeException("District can only create district staff / IA_ADMIN");
			}
		} else if (cr == Roles.IA_ADMIN) {
			// IA can optionally create nothing here (vendors are handled in VendorService)
			throw new RuntimeException("IA is not allowed to register users here");
		} else {
			throw new RuntimeException("Unauthorized creator role");
		}

		// basic validations
		if (dto.getUsername() == null || dto.getUsername().isBlank())
			throw new RuntimeException("Username is required");
		if (dto.getPassword() == null || dto.getPassword().isBlank())
			throw new RuntimeException("Password is required");
		if (userRepo.findByUsername(dto.getUsername()).isPresent())
			throw new RuntimeException("Username already taken");
		if (dto.getRole() == null)
			throw new RuntimeException("Role is required");

		// district scoping when needed
		District district = null;
		if (isDistrictAny(dto.getRole()) || dto.getRole() == Roles.IA_ADMIN || dto.getRole() == Roles.HADP_ADMIN) {
			Long did = dto.getDistrictId() != null ? dto.getDistrictId()
					: (isDistrictAny(cr) && creator.getDistrict() != null ? creator.getDistrict().getId() : null);
			if (did == null)
				throw new RuntimeException("districtId is required for this role");
			district = districtRepo.findById(did).orElseThrow(() -> new RuntimeException("District not found"));
		}

		// Create user
		User u = new User();
		u.setFullname(dto.getFullname());
		u.setUsername(dto.getUsername());
		u.setEmail(dto.getEmail());
		u.setDesignation(dto.getDesignation());
		u.setPassword(passwordEncoder.encode(dto.getPassword()));
		u.setMobile(dto.getMobile());
		u.setRole(dto.getRole());
		u.setStatus(Statuses.ACTIVE);
		u.setCreatedBy(createdBy);
		u.setDistrict(district);

		if (dto.getRole() == Roles.IA_ADMIN && isDistrictAny(cr)) {
			u.setSupervisor(creator);
		}

		User saved = userRepo.save(u);

		// Role-specific linking/creation
		switch (dto.getRole()) {
		case MLA -> {
			if (dto.getExistingMlaId() != null) {
				var mla = mlaRepo.findById(dto.getExistingMlaId())
						.orElseThrow(() -> new RuntimeException("Existing MLA not found"));
				mla.setUser(saved);
				mlaRepo.save(mla);
			} else {
				if (dto.getConstituencyId() == null || dto.getMlaName() == null || dto.getMlaName().isBlank())
					throw new RuntimeException("For new MLA, constituencyId and mlaName are required");
				var cons = constituencyRepo.findById(dto.getConstituencyId())
						.orElseThrow(() -> new RuntimeException("Constituency not found"));
				var mla = new MLA();
				mla.setMlaName(dto.getMlaName());
				mla.setParty(dto.getMlaParty());
				mla.setContactNumber(dto.getMlaContactNumber());
				mla.setEmail(dto.getMlaEmail());
				mla.setTerm(dto.getMlaTerm());
				mla.setStatus(Statuses.ACTIVE);
				mla.setConstituency(cons);
				mla.setUser(saved);
				mlaRepo.save(mla);
			}
		}
		case MLC -> {
			if (dto.getExistingMlcId() != null) {
				var mlc = mlcRepo.findById(dto.getExistingMlcId())
						.orElseThrow(() -> new RuntimeException("Existing MLC not found"));
				mlc.setUser(saved);
				mlcRepo.save(mlc);
			} else {
				if (dto.getMlcName() == null || dto.getMlcName().isBlank())
					throw new RuntimeException("For new MLC, mlcName is required");
				var mlc = new MLC();
				mlc.setMlcName(dto.getMlcName());
				mlc.setCategory(dto.getMlcCategory());
				mlc.setContactNumber(dto.getMlcContactNumber());
				mlc.setEmail(dto.getMlcEmail());
				mlc.setTerm(dto.getMlcTerm());
				mlc.setRegion(dto.getMlcRegion());
				mlc.setStatus(Statuses.ACTIVE);
				if (dto.getAccessibleDistrictIds() != null && !dto.getAccessibleDistrictIds().isEmpty()) {
					mlc.setAccessibleDistricts(districtRepo.findAllById(dto.getAccessibleDistrictIds()));
				}
				if (dto.getAccessibleConstituencyIds() != null && !dto.getAccessibleConstituencyIds().isEmpty()) {
					mlc.setAccessibleConstituencies(constituencyRepo.findAllById(dto.getAccessibleConstituencyIds()));
				}
				mlc.setUser(saved);
				mlcRepo.save(mlc);
			}
		}
		case HADP_ADMIN -> {
			if (dto.getTalukaId() != null) {
				var taluka = talukaRepo.findById(dto.getTalukaId())
						.orElseThrow(() -> new RuntimeException("Taluka not found"));
				// District is already set; taluka mapping can be used elsewhere as needed.
			}
		}
		default -> {
			/* district staff / IA_ADMIN: nothing extra */ }
		}

		// Optional bank capture → UserBankAccount (NOT in User)
		if (dto.getBankAccountNumber() != null && dto.getBankIfsc() != null) {
			var acc = new UserBankAccount();
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
