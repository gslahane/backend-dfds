package com.lsit.dfds.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.ForgotPasswordDto;
import com.lsit.dfds.dto.ResetCredentialsDto;
import com.lsit.dfds.dto.StateUserCreateDto;
import com.lsit.dfds.dto.UserBankAccountDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.dto.UserWithBankDetailsDto;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCTerm;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.exception.MlaAlreadyExistsException;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.MLCTermRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserBankAccountRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

//com.lsit.dfds.service.UserServiceImpl
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final SchemesRepository schemesRepository;

	private final UserRepository userRepo;
	private final DistrictRepository districtRepo;
	private final ConstituencyRepository constituencyRepo;
	private final MLARepository mlaRepo;
	private final MLCRepository mlcRepo;
	private final TalukaRepository talukaRepo;
	private final PasswordEncoder passwordEncoder;
	private final BankAccountService bankAccountService; // you already created this helper
	private final AuditService audit;
	private final UserBankAccountRepository bankAccountRepository;
	private final MLCTermRepository mlcTermRepository;
	private final MLATermRepository mlaTermRepository;
	private final DistrictRepository districtRepository;

	private final SchemesRepository schemesRepo;
	private final FileStorageService fileStorageService;
	// Strong password policy:
	// - min 8 chars
	// - at least 1 upper, 1 lower, 1 digit, 1 special
	private static final Pattern UPPER = Pattern.compile(".*[A-Z].*");
	private static final Pattern LOWER = Pattern.compile(".*[a-z].*");
	private static final Pattern DIGIT = Pattern.compile(".*\\d.*");
	private static final Pattern SPECIAL = Pattern.compile(".*[^A-Za-z0-9].*");

//	UserServiceImpl(SchemesRepository schemesRepository) {
//		this.schemesRepository = schemesRepository;
//	}

	private static boolean isState(Roles r) {
		return r != null && r.name().startsWith("STATE");
	}

	private static boolean isDistrictAny(Roles r) {
		return r != null && r.name().startsWith("DISTRICT");
	}

	private UserDto toUserDto(User user) {
		UserDto dto = new UserDto();
		dto.setUserId(user.getId());
		dto.setFullname(user.getFullname());
		dto.setUsername(user.getUsername());
		dto.setMobile(user.getMobile());
		dto.setEmail(user.getEmail());
		dto.setDesignation(user.getDesignation());
		dto.setRole(user.getRole());
		dto.setStatus(user.getStatus());
		if (user.getDistrict() != null) {
			dto.setDistrictId(user.getDistrict().getId());
			dto.setDistrictName(user.getDistrict().getDistrictName());
		}
		// Add MLA/MLC specific data
		if (user.getRole() == Roles.MLA) {
			MLA mla = mlaRepo.findByUser_Id(user.getId()).orElse(null);
			if (mla != null) {
				dto.setMlaId(mla.getId());
				dto.setMlaName(mla.getMlaName());
				dto.setMlaParty(mla.getParty());
				dto.setMlaContactNumber(mla.getContactNumber());
				dto.setMlaStatus(mla.getStatus());
				dto.setMlaStatusChangeLetterUrl(mla.getStatusChangeLetterUrl());
				if (mla.getConstituency() != null) {
					dto.setConstituencyId(mla.getConstituency().getId());
					dto.setConstituencyName(mla.getConstituency().getConstituencyName());
				}
				// Set MLA term number from MLATerm
				var mlaTerm = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId()).orElse(null);
				if (mlaTerm != null) {
					dto.setMlaTerm(mlaTerm.getTermNumber());
				}
			}
		} else if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepo.findByUser_Id(user.getId()).orElse(null);
			if (mlc != null) {
				dto.setMlcId(mlc.getId());
				dto.setMlcName(mlc.getMlcName());
				dto.setMlcCategory(mlc.getCategory());
				dto.setMlcContactNumber(mlc.getContactNumber());
				dto.setMlcRegion(mlc.getRegion());
				dto.setMlcStatus(mlc.getStatus());
				dto.setMlcStatusChangeLetterUrl(mlc.getStatusChangeLetterUrl());
				// Set MLC term number and tenure info from MLCTerm
				MLCTerm mlcTerm = mlcTermRepository.findFirstByMlc_IdAndActiveTrue(mlc.getId()).orElse(null);
				if (mlcTerm != null) {
					dto.setMlcTerm(mlcTerm.getTermNumber());
					dto.setTenureStartDate(mlcTerm.getTenureStartDate());
					dto.setTenureEndDate(mlcTerm.getTenureEndDate());
					dto.setTenurePeriod(mlcTerm.getTenurePeriod());
				}
			}
		}
		return dto;
	}

	// src/main/java/com/lsit/dfds/service/UserServiceImpl.java
	@Override
	@Transactional
	public UserDto registerStateUser(StateUserCreateDto dto, String createdBy) {
		// Creator permissions
		User creator = userRepo.findByUsername(createdBy).orElseThrow(() -> new RuntimeException("Creator not found"));
		if (creator.getRole() != Roles.STATE_ADMIN) {
			throw new RuntimeException("Only STATE_ADMIN can create state users");
		}

		// Validate role
		if (dto.getRole() == null) {
			throw new RuntimeException("role is required");
		}
		if (!(dto.getRole() == Roles.STATE_ADMIN || dto.getRole() == Roles.STATE_CHECKER
				|| dto.getRole() == Roles.STATE_MAKER)) {
			throw new RuntimeException("Invalid role for /state registration");
		}

		// Uniqueness
		if (userRepo.findByUsername(dto.getUsername()).isPresent()) {
			throw new RuntimeException("Username already taken");
		}

		// Optional supervisor must be STATE_*
		User supervisor = null;
		if (dto.getSupervisorUserId() != null) {
			supervisor = userRepo.findById(dto.getSupervisorUserId())
					.orElseThrow(() -> new RuntimeException("Supervisor user not found"));
			if (!isState(supervisor.getRole())) {
				throw new RuntimeException("Supervisor must be a STATE_* user");
			}
		}

		// Create user
		User u = new User();
		u.setFullname(dto.getFullname());
		u.setUsername(dto.getUsername());
		u.setEmail(dto.getEmail());
		u.setDesignation(dto.getDesignation());
		u.setPassword(passwordEncoder.encode(dto.getPassword()));
		u.setMobile(dto.getMobile() == null ? null : Long.valueOf(dto.getMobile()));
		u.setRole(dto.getRole());
		u.setStatus(Statuses.ACTIVE);
		u.setCreatedBy(createdBy);
		u.setSupervisor(supervisor);
		// state users have district = null

		User saved = userRepo.save(u);

		// ---------- NEW: Optional bank capture ----------
		if (dto.getBankAccountNumber() != null && dto.getBankIfsc() != null) {
			var acc = new UserBankAccount();
			acc.setUser(saved);
			acc.setAccountHolderName(saved.getFullname());
			acc.setAccountNumber(dto.getBankAccountNumber());
			acc.setIfsc(dto.getBankIfsc());
			acc.setBankName(dto.getBankName());
			acc.setBranch(dto.getBankBranch());
			acc.setAccountType(dto.getAccountType()); // "SB" / "CA"
			bankAccountService.setPrimary(saved.getId(), acc, createdBy);
		}

		audit.log(createdBy, "REGISTER_STATE_USER", "User", saved.getId(),
				"{\"username\":\"" + saved.getUsername() + "\",\"role\":\"" + saved.getRole() + "\"}");

		return toUserDto(saved);
	}

	@Override
	@Transactional
	public User registerDistrictUser(UserRegistrationDto dto, String createdBy) {
		// Force role group
		if (dto.getRole() == null) {
			throw new RuntimeException("role is required");
		}
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
		if (dto.getUsername() == null || dto.getUsername().isBlank()) {
			throw new RuntimeException("Username is required");
		}
		if (dto.getPassword() == null || dto.getPassword().isBlank()) {
			throw new RuntimeException("Password is required");
		}
		if (userRepo.findByUsername(dto.getUsername()).isPresent()) {
			throw new RuntimeException("Username already taken");
		}
		if (dto.getRole() == null) {
			throw new RuntimeException("Role is required");
		}

		// district scoping when needed
		District district = null;
		if (isDistrictAny(dto.getRole()) || dto.getRole() == Roles.IA_ADMIN || dto.getRole() == Roles.HADP_ADMIN) {
			Long did = dto.getDistrictId() != null ? dto.getDistrictId()
					: (isDistrictAny(cr) && creator.getDistrict() != null ? creator.getDistrict().getId() : null);
			if (did == null) {
				throw new RuntimeException("districtId is required for this role");
			}
			district = districtRepo.findById(did).orElseThrow(() -> new RuntimeException("District not found"));
		}

		// MLA active check before user creation
		if (dto.getRole() == Roles.MLA && dto.getConstituencyId() != null) {
			var activeMlaOpt = mlaRepo.findByConstituency_IdAndStatus(dto.getConstituencyId(), Statuses.ACTIVE);
			if (activeMlaOpt.isPresent()) {
				throw new MlaAlreadyExistsException("An active MLA already exists for this constituency.");
			}
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
//		u.setSupervisor(creator.getSupervisor().getId()); // inherit creator's supervisor
		District district1 = null;
		// set district from ID
		if (dto.getDistrictId() != null) {
			district1 = districtRepository.findById(dto.getDistrictId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid districtId: " + dto.getDistrictId()));
			// OR: District district = em.getReference(District.class, dto.getDistrictId());
			u.setDistrict(district1);
		} else {
			u.setDistrict(null);
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
				if (dto.getConstituencyId() == null || dto.getMlaName() == null || dto.getMlaName().isBlank()) {
					throw new RuntimeException("For new MLA, constituencyId and mlaName are required");
				}
				// Check if an active MLA already exists for this constituency
				var activeMlaOpt = mlaRepo.findByConstituency_IdAndStatus(dto.getConstituencyId(), Statuses.ACTIVE);
				if (activeMlaOpt.isPresent()) {
					throw new RuntimeException("An active MLA already exists for this constituency.");
				}
				var cons = constituencyRepo.findById(dto.getConstituencyId())
						.orElseThrow(() -> new RuntimeException("Constituency not found"));
				var mla = new MLA();
				mla.setMlaName(dto.getMlaName());
				mla.setParty(dto.getMlaParty());
				mla.setContactNumber(dto.getMlaContactNumber());
				mla.setEmail(dto.getMlaEmail());
				mla.setStatus(Statuses.ACTIVE);
				mla.setConstituency(cons);

				// ⬇️ NEW: set default scheme at backend (uses dto.getDefaultSchemeId() if
				// ✅ Fetch the single MLA plan scheme and set as default
				Schemes defaultMlaScheme = schemesRepo.findFirstByPlanTypeOrderByIdAsc(PlanType.MLA)
						.orElseThrow(() -> new RuntimeException("Default MLA scheme not configured"));
				mla.setDefaultScheme(defaultMlaScheme);
				mla.setUser(saved);
				mlaRepo.save(mla);

				// Create MLA Term if tenure dates provided
				if (dto.getMlaTenureStartDate() != null && dto.getMlaTenureEndDate() != null) {
					var mlaTerm = new MLATerm();
					mlaTerm.setMla(mla);
					mlaTerm.setActive(true);
					mlaTerm.setTenureStartDate(dto.getMlaTenureStartDate());
					mlaTerm.setTenureEndDate(dto.getMlaTenureEndDate());
					// Calculate tenure period in years
					double years = java.time.Period.between(dto.getMlaTenureStartDate(), dto.getMlaTenureEndDate())
							.getYears();
					mlaTerm.setTenurePeriod(Double.toString(years));
					mlaTerm.setTermNumber(dto.getMlaTerm() != null ? dto.getMlaTerm() : 1);
					mlaTermRepository.save(mlaTerm);
				}
			}
		}
		case MLC -> {
			if (dto.getExistingMlcId() != null) {
				var mlc = mlcRepo.findById(dto.getExistingMlcId())
						.orElseThrow(() -> new RuntimeException("Existing MLC not found"));
				mlc.setUser(saved);
				mlcRepo.save(mlc);
			} else {
				if (dto.getMlcName() == null || dto.getMlcName().isBlank()) {
					throw new RuntimeException("For new MLC, mlcName is required");
				}
				var mlc = new MLC();
				mlc.setMlcName(dto.getMlcName());
				mlc.setCategory(dto.getMlcCategory());
				mlc.setContactNumber(dto.getMlcContactNumber());
				mlc.setEmail(dto.getMlcEmail());
				mlc.setRegion(dto.getMlcRegion());
				mlc.setDistrict(district1); 
				mlc.setStatus(Statuses.ACTIVE);
				if (dto.getAccessibleDistrictIds() != null && !dto.getAccessibleDistrictIds().isEmpty()) {
					mlc.setAccessibleDistricts(districtRepo.findAllById(dto.getAccessibleDistrictIds()));
				}
				if (dto.getAccessibleConstituencyIds() != null && !dto.getAccessibleConstituencyIds().isEmpty()) {
					mlc.setAccessibleConstituencies(constituencyRepo.findAllById(dto.getAccessibleConstituencyIds()));
				}
				mlc.setUser(saved);
				mlcRepo.save(mlc);

				// Create MLC Term if tenure dates provided
				if (dto.getMlcTenureStartDate() != null && dto.getMlcTenureEndDate() != null) {
					var mlcTerm = new MLCTerm();
					mlcTerm.setMlc(mlc);
					mlcTerm.setActive(true);
					mlcTerm.setTenureStartDate(dto.getMlcTenureStartDate());
					mlcTerm.setTenureEndDate(dto.getMlcTenureEndDate());
					mlcTerm.setTermNumber(dto.getMlcTerm() != null ? dto.getMlcTerm() : 1);
					mlcTermRepository.save(mlcTerm);
				}
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

	@Override
	public List<UserWithBankDetailsDto> getIAUsersWithBankDetails(String requesterUsername, Long districtId) {
		User requester = userRepo.findByUsername(requesterUsername)
				.orElseThrow(() -> new RuntimeException("User not found: " + requesterUsername));

		Roles role = requester.getRole();
		boolean isStateUser = role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER;
		boolean isDistrictUser = role == Roles.DISTRICT_COLLECTOR || role == Roles.DISTRICT_DPO
				|| role == Roles.DISTRICT_ADPO || role == Roles.DISTRICT_ADMIN || role == Roles.DISTRICT_CHECKER
				|| role == Roles.DISTRICT_MAKER;

		// ✅ final/effectively-final variable for lambda capture
		final Long scopeDistrictId;
		if (isStateUser) {
			// STATE: can see all; optional filter by ?districtId=
			scopeDistrictId = districtId; // may be null => all districts
		} else if (isDistrictUser) {
			// DISTRICT: force to own district
			if (requester.getDistrict() == null) {
				throw new RuntimeException("Logged-in user is not mapped to any district");
			}
			scopeDistrictId = requester.getDistrict().getId();
		} else {
			// Other roles (e.g., IA): restrict to their own district if available, else no
			// results
			if (requester.getDistrict() == null) {
				return List.of();
			}
			scopeDistrictId = requester.getDistrict().getId();
		}

		List<User> iaUsers = userRepo.findAll().stream()
				.filter(u -> u.getRole() != null && u.getRole().name().startsWith("IA_"))
				.filter(u -> scopeDistrictId == null
						|| (u.getDistrict() != null && Objects.equals(u.getDistrict().getId(), scopeDistrictId)))
				.collect(Collectors.toList());

		return iaUsers.stream().map(user -> {
			UserWithBankDetailsDto dto = new UserWithBankDetailsDto();
			dto.setId(user.getId());
			dto.setFullname(user.getFullname());
			dto.setUsername(user.getUsername());
			dto.setMobile(user.getMobile());
			dto.setEmail(user.getEmail());
			dto.setDesignation(user.getDesignation());
			dto.setRole(user.getRole());
			dto.setStatus(user.getStatus());
			dto.setCreatedAt(user.getCreatedAt());
			dto.setCreatedBy(user.getCreatedBy());
			dto.setDistrictName(user.getDistrict() != null ? user.getDistrict().getDistrictName() : null);

			List<UserBankAccount> accounts = bankAccountRepository.findByUserId(user.getId());
			dto.setBankAccounts(accounts.stream().map(acc -> {
				UserBankAccountDto b = new UserBankAccountDto();
				b.setAccountHolderName(acc.getAccountHolderName());
				b.setAccountNumber(acc.getAccountNumber());
				b.setIfsc(acc.getIfsc());
				b.setBankName(acc.getBankName());
				b.setBranch(acc.getBranch());
				b.setAccountType(acc.getAccountType());
				b.setIsPrimary(acc.getIsPrimary());
				b.setVerified(acc.getVerified());
				b.setStatus(acc.getStatus());
				return b;
			}).collect(Collectors.toList()));

			return dto;
		}).collect(Collectors.toList());
	}

	private void enforcePasswordPolicy(String username, String newPassword) {
		if (newPassword == null || newPassword.length() < 8)
			throw new RuntimeException("Password must be at least 8 characters");
		if (!UPPER.matcher(newPassword).matches())
			throw new RuntimeException("Password must contain at least one uppercase letter");
		if (!LOWER.matcher(newPassword).matches())
			throw new RuntimeException("Password must contain at least one lowercase letter");
		if (!DIGIT.matcher(newPassword).matches())
			throw new RuntimeException("Password must contain at least one digit");
		if (!SPECIAL.matcher(newPassword).matches())
			throw new RuntimeException("Password must contain at least one special character");
		String uname = (username == null) ? "" : username.toLowerCase();
		if (newPassword.toLowerCase().contains(uname))
			throw new RuntimeException("Password must not contain the username");
		if (newPassword.toLowerCase().contains("password"))
			throw new RuntimeException("Password must not contain the word 'password'");
	}

	// ---------- unauthenticated – "Forgot Password" ----------
	@Override
	@Transactional
	public void forgotPassword(ForgotPasswordDto dto) {
		if ((dto.getEmail() == null || dto.getEmail().isBlank()) && (dto.getMobile() == null)) {
			throw new RuntimeException("Provide either email or mobile for verification");
		}

		User user = userRepo.findByUsernameIgnoreCase(dto.getUsername())
				.orElseThrow(() -> new RuntimeException("No user found for username"));

		// Basic verification (OTP to be integrated later)
		boolean emailOk = (dto.getEmail() != null && user.getEmail() != null
				&& user.getEmail().equalsIgnoreCase(dto.getEmail()));
		boolean mobileOk = (dto.getMobile() != null && user.getMobile() != null
				&& user.getMobile().equals(dto.getMobile()));

		if (!emailOk && !mobileOk) {
			throw new RuntimeException(
					"Verification failed. (When Email/WhatsApp OTP is integrated, this step will be done via OTP.)");
		}

		if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}

		enforcePasswordPolicy(user.getUsername(), dto.getNewPassword());

		// Update password
		user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
		// Optional: add a timestamp field if you want to invalidate existing tokens in
		// filters
		// e.g., user.setLastCredentialsResetAt(LocalDateTime.now());
		userRepo.save(user);
	}

	// ---------- authenticated – reset username/password ----------
	@Override
	@Transactional
	public void resetCredentials(String authUsername, ResetCredentialsDto dto) {
		User user = userRepo.findByUsername(authUsername)
				.orElseThrow(() -> new RuntimeException("Authenticated user not found"));

		// verify current password
		if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
			throw new RuntimeException("Current password is incorrect");
		}

		if (!dto.getNewPassword().equals(dto.getConfirmNewPassword())) {
			throw new RuntimeException("New password and confirm password do not match");
		}

		enforcePasswordPolicy(user.getUsername(), dto.getNewPassword());

		// Optional username change
		if (dto.getNewUsername() != null && !dto.getNewUsername().isBlank()
				&& !dto.getNewUsername().equalsIgnoreCase(user.getUsername())) {
			if (userRepo.existsByUsernameIgnoreCase(dto.getNewUsername())) {
				throw new RuntimeException("Username is already taken");
			}
			user.setUsername(dto.getNewUsername().trim());
		}

		// Set new password
		String encoded = passwordEncoder.encode(dto.getNewPassword());
		if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
			// extremely unlikely, but if encoder produces same hash (e.g., user re-uses old
			// password)
			throw new RuntimeException("New password cannot be same as current password");
		}
		user.setPassword(encoded);

		// If you maintain “session invalidation”, set a marker time here.
		// You could add a column like `lastCredentialsResetAt` to User and check in JWT
		// filter.
		// user.setLastCredentialsResetAt(LocalDateTime.now());

		userRepo.save(user);
	}

	@Override
	public String setStatusDeactivate(String username, Long id, MultipartFile letter) {
		User requestUser = userRepo.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("Authenticated user not found"));
		if (requestUser.getRole() != Roles.STATE_ADMIN && requestUser.getRole() != Roles.STATE_CHECKER
				&& requestUser.getRole() != Roles.STATE_MAKER) {
			throw new RuntimeException("Only STATE_ADMIN or STATE_CHECKER or STATE_MAKER can deactivate MLC or MLA");
		}

		User user = userRepo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
		if (user.getRole() != Roles.MLC && user.getRole() != Roles.MLA) {
			throw new RuntimeException("Deactivation is only applicable for MLC and MLA users");
		}

		// Only allow deactivation if currently ACTIVE
		if (user.getStatus() != Statuses.ACTIVE) {
			throw new RuntimeException("User is already inactive");
		}

		// Save file and get URL
		String uploadedUrl = (letter != null && !letter.isEmpty()) ? fileStorageService.saveFile(letter,
				user.getRole() == Roles.MLC ? "mlc-status-letters" : "mla-status-letters") : null;
		if (uploadedUrl == null) {
			throw new RuntimeException("Deactivation letter is required (upload a file or provide a URL).");
		}

		// Set status to INACTIVE and update letter URL for MLA/MLC and their terms
		if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepo.findByUser_Id(user.getId())
					.orElseThrow(() -> new RuntimeException("MLC record not found for user"));
			mlc.setStatus(Statuses.INACTIVE);
			mlc.setStatusChangeLetterUrl(uploadedUrl);
			mlcRepo.save(mlc);
			// Deactivate active MLCTerm if exists
			MLCTerm mlcTerm = mlcTermRepository.findFirstByMlc_IdAndActiveTrue(mlc.getId()).orElse(null);
			if (mlcTerm != null) {
				mlcTerm.setActive(false);
				mlcTermRepository.save(mlcTerm);
			}
		} else if (user.getRole() == Roles.MLA) {
			MLA mla = mlaRepo.findByUser_Id(user.getId())
					.orElseThrow(() -> new RuntimeException("MLA record not found for user"));
			mla.setStatus(Statuses.INACTIVE);
			mla.setStatusChangeLetterUrl(uploadedUrl);
			mlaRepo.save(mla);
			// Deactivate active MLATerm if exists
			MLATerm mlaTerm = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId()).orElse(null);
			if (mlaTerm != null) {
				mlaTerm.setActive(false);
				mlaTermRepository.save(mlaTerm);
			}
		}

		// Update User status
		user.setStatus(Statuses.INACTIVE);
		userRepo.save(user);

		audit.log(username, "SET_STATUS_DEACTIVATE", "User", user.getId(),
				"{\"newStatus\":\"INACTIVE\",\"statusChangeLetterUrl\":\"" + uploadedUrl + "\"}");

		return "User deactivated successfully.";
	}

}