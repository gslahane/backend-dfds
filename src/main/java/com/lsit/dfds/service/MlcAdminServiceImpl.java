// src/main/java/com/lsit/dfds/service/MlcAdminServiceImpl.java
package com.lsit.dfds.service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlcInfoDto;
import com.lsit.dfds.dto.MlcListFilterDto;
import com.lsit.dfds.dto.MlcStatusUpdateDto;
import com.lsit.dfds.dto.MlcUpdateDto;
import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCTerm;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.MLCTermRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MlcAdminServiceImpl implements MlcAdminService {

	private final MLCRepository mlcRepo;
	private final UserRepository userRepo;
	private final DistrictRepository districtRepo;
	private final ConstituencyRepository constituencyRepo;
	private final SchemesRepository schemesRepo;
	private final FileStorageService fileStorageService;
	private final MLCTermRepository mlcTermRepo;

	// -------------------- API: LIST --------------------
	@Override
	@Transactional(readOnly = true)
	public List<MlcInfoDto> list(String username, MlcListFilterDto filter) {
		User caller = me(username);

		Long districtFilter = filter != null ? filter.getDistrictId() : null;
		String statusStr = filter != null ? nz(filter.getStatus()) : null;
		String search = filter != null ? nz(filter.getSearch()) : null;

		// District / IA users default to their district if not specified
		if ((isDistrict(caller) || isIa(caller)) && districtFilter == null) {
			if (caller.getDistrict() == null) {
				throw new RuntimeException("Caller not mapped to a district");
			}
			districtFilter = caller.getDistrict().getId();
		}

		List<MLC> all = mlcRepo.findAll();

		// Filters (make captured vars final/effectively final)
		final Statuses statusFilter = parseStatuses(statusStr);
		final String needle = (search != null && !search.isBlank()) ? search.toLowerCase(Locale.ROOT) : null;
		final Long finalDistrictFilter = districtFilter;

		List<MLC> filtered = all.stream().filter(m -> statusFilter == null || statusFilter == m.getStatus())
				.filter(m -> {
					if (finalDistrictFilter == null) {
						return true;
					}
					return m.getAccessibleDistricts() != null && m.getAccessibleDistricts().stream()
							.anyMatch(d -> Objects.equals(d.getId(), finalDistrictFilter));
				}).filter(m -> {
					if (needle == null) {
						return true;
					}
					String name = nz(m.getMlcName()).toLowerCase(Locale.ROOT);
					String email = nz(m.getEmail()).toLowerCase(Locale.ROOT);
					String phone = nz(m.getContactNumber()).toLowerCase(Locale.ROOT);
					return name.contains(needle) || email.contains(needle) || phone.contains(needle);
				}).filter(m -> canView(caller, m)).toList();

		return filtered.stream().map(m -> toDto(m, mlcTermRepo)).toList();
	}

	// -------------------- API: GET ONE --------------------
	@Override
	@Transactional(readOnly = true)
	public MlcInfoDto getOne(String username, Long id) {
		User caller = me(username);
		MLC mlc = mlcRepo.findById(id).orElseThrow(() -> new RuntimeException("MLC not found"));
		ensureCanView(caller, mlc);
		return toDto(mlc, mlcTermRepo);
	}

	// -------------------- API: UPDATE --------------------
	@Override
	@Transactional
	public MlcInfoDto update(String username, Long id, MlcUpdateDto dto) {
		User caller = me(username);
		MLC mlc = mlcRepo.findById(id).orElseThrow(() -> new RuntimeException("MLC not found"));
		ensureCanEdit(caller, mlc);

		if (dto.getMlcName() != null) {
			mlc.setMlcName(dto.getMlcName());
		}
		if (dto.getCategory() != null) {
			mlc.setCategory(dto.getCategory());
		}
		if (dto.getContactNumber() != null) {
			mlc.setContactNumber(dto.getContactNumber());
		}
		if (dto.getEmail() != null) {
			mlc.setEmail(dto.getEmail());
		}

		if (dto.getAccessibleDistrictIds() != null) {
			List<District> dists = dto.getAccessibleDistrictIds().isEmpty() ? Collections.emptyList()
					: districtRepo.findAllById(dto.getAccessibleDistrictIds());
			mlc.setAccessibleDistricts(dists);
		}

		if (dto.getAccessibleConstituencyIds() != null) {
			List<Constituency> cons = dto.getAccessibleConstituencyIds().isEmpty() ? Collections.emptyList()
					: constituencyRepo.findAllById(dto.getAccessibleConstituencyIds());
			mlc.setAccessibleConstituencies(cons);
		}

		// ------ DEFAULT SCHEME HANDLING (final variable) ------
		final Long defaultSchemeIdVal = resolveDefaultSchemeId(dto);
		if (defaultSchemeIdVal != null) {
			Schemes s = schemesRepo.findById(defaultSchemeIdVal)
					.orElseThrow(() -> new RuntimeException("Scheme not found: " + defaultSchemeIdVal));
			mlc.setDefaultScheme(s);
		}
		// If null, we keep the existing defaultScheme. (Field is non-nullable.)

		MLC saved = mlcRepo.save(mlc);
		return toDto(saved, mlcTermRepo);
	}

	// Resolve default scheme id either from dto.getDefaultSchemeId() (if exists)
	// or first element of dto.getSchemeIds() (back-compat). No lambdas here.
	private Long resolveDefaultSchemeId(MlcUpdateDto dto) {
		if (dto == null) {
			return null;
		}

		// Try reflective getter (won't break compile if method doesn't exist)
		try {
			var m = dto.getClass().getMethod("getDefaultSchemeId");
			Object val = m.invoke(dto);
			if (val instanceof Long l) {
				return l;
			}
			if (val instanceof Number n) {
				return n.longValue();
			}
		} catch (NoSuchMethodException ignore) {
			// DTO doesn't have getDefaultSchemeId(): fall through
		} catch (Exception e) {
			throw new RuntimeException("Failed to read defaultSchemeId from DTO", e);
		}

		// Fallback to first of schemeIds (if present)
		if (dto.getSchemeIds() != null && !dto.getSchemeIds().isEmpty()) {
			return dto.getSchemeIds().get(0);
		}
		return null;
	}

	// -------------------- API: SET STATUS (with letter) --------------------
	@Override
	@Transactional
	public String setStatus(String username, Long id, Statuses status, MlcStatusUpdateDto dto, MultipartFile letter) {
		User caller = me(username);
		if (status == null) {
			throw new RuntimeException("Status is required");
		}

		MLC mlc = mlcRepo.findById(id).orElseThrow(() -> new RuntimeException("MLC not found"));
		ensureCanEdit(caller, mlc);

		String uploadedUrl = (letter != null && !letter.isEmpty())
				? fileStorageService.saveFile(letter, "mlc-status-letters")
				: null;
		String payloadUrl = (dto != null) ? nz(dto.getStatusChangeLetterUrl()) : null;
		String finalUrl = (uploadedUrl != null) ? uploadedUrl
				: (payloadUrl != null && !payloadUrl.isBlank() ? payloadUrl : null);

		if (finalUrl == null) {
			throw new RuntimeException("Status change letter is required (upload a file or provide a URL).");
		}

		mlc.setStatus(status);
		mlc.setStatusChangeLetterUrl(finalUrl);

		if (dto != null && Boolean.TRUE.equals(dto.getAlsoToggleUser()) && mlc.getUser() != null) {
			mlc.getUser().setStatus(status);
			userRepo.save(mlc.getUser());
		}

		mlcRepo.save(mlc);
		return "Status updated to " + status.name();
	}

	// -------------------- API: SET STATUS (simple overload) --------------------
	@Override
	@Transactional
	public String setStatus(String username, Long id, Statuses status) {
		User caller = me(username);
		if (status == null) {
			throw new RuntimeException("Status is required");
		}

		MLC mlc = mlcRepo.findById(id).orElseThrow(() -> new RuntimeException("MLC not found"));
		ensureCanEdit(caller, mlc);

		mlc.setStatus(status);
		mlcRepo.save(mlc);
		return "Status updated to " + status.name();
	}

	// -------------------- Helpers --------------------
	private User me(String username) {
		return userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	private static boolean isState(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private static boolean isDistrict(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("DISTRICT");
	}

	private static boolean isIa(User u) {
		return u.getRole() == Roles.IA_ADMIN;
	}

	private static boolean isMlc(User u) {
		return u.getRole() == Roles.MLC;
	}

	private boolean canView(User caller, MLC mlc) {
		try {
			ensureCanView(caller, mlc);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	private void ensureCanView(User caller, MLC mlc) {
		if (isState(caller)) {
			return;
		}

		if (isMlc(caller)) {
			Long callerMlcId = mlcRepo.findByUser_Id(caller.getId()).map(MLC::getId).orElse(null);
			if (Objects.equals(mlc.getId(), callerMlcId)) {
				return;
			}
			throw new RuntimeException("Forbidden");
		}

		if (isDistrict(caller) || isIa(caller)) {
			if (caller.getDistrict() == null) {
				throw new RuntimeException("Caller not mapped to a district");
			}
			Long did = caller.getDistrict().getId();
			boolean ok = mlc.getAccessibleDistricts() != null
					&& mlc.getAccessibleDistricts().stream().anyMatch(d -> Objects.equals(d.getId(), did));
			if (ok) {
				return;
			}
			throw new RuntimeException("Forbidden");
		}

		throw new RuntimeException("Forbidden");
	}

	private void ensureCanEdit(User caller, MLC mlc) {
		if (isState(caller)) {
			return;
		}

		if (isDistrict(caller)) {
			String r = caller.getRole().name();
			boolean leadership = r.equals("DISTRICT_COLLECTOR") || r.equals("DISTRICT_DPO") || r.equals("DISTRICT_ADPO")
					|| r.equals("DISTRICT_ADMIN");
			if (!leadership) {
				throw new RuntimeException("Only district leadership can edit MLC");
			}
			ensureCanView(caller, mlc);
			return;
		}

		throw new RuntimeException("Forbidden");
	}

	private static String nz(String s) {
		return s == null ? "" : s.trim();
	}

	private static Statuses parseStatuses(String s) {
		if (s == null || s.isBlank()) {
			return null;
		}
		try {
			return Statuses.valueOf(s.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Invalid status: " + s);
		}
	}

	// Mapper: returns singleton lists for scheme ids/names to keep DTO shape
	private static MlcInfoDto toDto(MLC m, MLCTermRepository mlcTermRepo) {
		List<Long> distIds = m.getAccessibleDistricts() == null ? List.of()
				: m.getAccessibleDistricts().stream().map(District::getId).toList();
		List<String> distNames = m.getAccessibleDistricts() == null ? List.of()
				: m.getAccessibleDistricts().stream().map(District::getDistrictName).toList();

		List<Long> conIds = m.getAccessibleConstituencies() == null ? List.of()
				: m.getAccessibleConstituencies().stream().map(Constituency::getId).toList();
		List<String> conNames = m.getAccessibleConstituencies() == null ? List.of()
				: m.getAccessibleConstituencies().stream().map(Constituency::getConstituencyName).toList();

		List<Long> schemeIds = (m.getDefaultScheme() != null && m.getDefaultScheme().getId() != null)
				? List.of(m.getDefaultScheme().getId())
				: List.of();
		List<String> schemeNames = (m.getDefaultScheme() != null && m.getDefaultScheme().getSchemeName() != null)
				? List.of(m.getDefaultScheme().getSchemeName())
				: List.of();

		Long userId = m.getUser() != null ? m.getUser().getId() : null;
		String username = m.getUser() != null ? m.getUser().getUsername() : null;

		Long districtId = m.getDistrict() != null ? m.getDistrict().getId() : null;
		String districtName = m.getDistrict() != null ? m.getDistrict().getDistrictName() : null;

		// Fetch current active MLCTerm
		MLCTerm term = mlcTermRepo.findFirstByMlc_IdAndActiveTrue(m.getId()).orElse(null);
		Integer termNumber = term != null ? term.getTermNumber() : null;
		java.time.LocalDate tenureStartDate = term != null ? term.getTenureStartDate() : null;
		java.time.LocalDate tenureEndDate = term != null ? term.getTenureEndDate() : null;
		String tenurePeriod = term != null ? term.getTenurePeriod() : null;

		return new MlcInfoDto(m.getId(), m.getMlcName(), m.getCategory(), m.getContactNumber(), m.getEmail(),
				termNumber, tenureStartDate, tenureEndDate, tenurePeriod, m.getStatus(), distIds, distNames, conIds,
				conNames, schemeIds, schemeNames, userId, username, districtId, districtName);
	}
}