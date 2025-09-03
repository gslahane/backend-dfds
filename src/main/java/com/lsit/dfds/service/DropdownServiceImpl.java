package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.SchemeTypeDropdownDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.DivisionRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.SectorRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.WorkRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DropdownServiceImpl implements DropdownService {

	private final UserRepository userRepository;
	private final SchemesRepository schemesRepository;
	private final SectorRepository sectorRepository;
	private final DemandCodeRepository schemeTypeRepository;

	@Autowired
	private DivisionRepository divisionRepo;

	@Autowired
	private DistrictRepository districtRepo;

	@Autowired
	private FinancialYearRepository financialYearRepository;

	@Autowired
	private WorkRepository workRepository;

	@Autowired
	private TalukaRepository talukaRepo;

	@Autowired
	private ConstituencyRepository constituencyRepo;

	@Autowired
	private MLARepository mlaRepository;

	@Autowired
	private MLCRepository mlcRepository;

	@Override
	public List<String> getSchemeNamesForUser(String username) {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new RuntimeException("User not found: " + username));

		Roles role = user.getRole();
		if (role == null) {
			throw new RuntimeException("Role not mapped for user: " + username);
		}

		// ---- STATE (all state roles)
		if (role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER) {
			return schemesRepository.findAll().stream().map(Schemes::getSchemeName).distinct()
					.collect(Collectors.toList());
		}

		// ---- MLA / MLA_REP (return default MLA scheme)
		if (role == Roles.MLA || role == Roles.MLA_REP) {
			MLA mla = mlaRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new RuntimeException("MLA record not linked to user: " + username));
			Schemes s = resolveDefaultSchemeForMla(mla);
			return List.of(s.getSchemeName());
		}

		// ---- MLC (return default MLC scheme)
		if (role == Roles.MLC) {
			MLC mlc = mlcRepository.findByUser_Id(user.getId())
					.orElseThrow(() -> new RuntimeException("MLC record not linked to user: " + username));
			Schemes s = resolveDefaultSchemeForMlc(mlc);
			return List.of(s.getSchemeName());
		}

		// ---- DISTRICT & IA (scoped to user's district)
		if (role == Roles.DISTRICT_ADMIN || role == Roles.DISTRICT_CHECKER || role == Roles.DISTRICT_MAKER
				|| role == Roles.DISTRICT_ADPO || role == Roles.DISTRICT_DPO || role == Roles.DISTRICT_COLLECTOR
				|| role == Roles.IA_ADMIN) {

			if (user.getDistrict() == null) {
				throw new RuntimeException("District not mapped for user: " + username);
			}
			Long districtId = user.getDistrict().getId();

			return schemesRepository.findByDistricts_Id(districtId).stream().map(Schemes::getSchemeName).distinct()
					.collect(Collectors.toList());
		}

		throw new RuntimeException("Unsupported role for dropdown: " + role);
	}

	/* Helpers (put in the same service class) */
	private Schemes resolveDefaultSchemeForMla(MLA mla) {
		// Prefer field named "defaultScheme", fallback "scheme"
		Schemes s = tryReadSchemeField(mla, "defaultScheme");
		if (s == null)
			s = tryReadSchemeField(mla, "scheme");
		if (s == null || s.getId() == null) {
			throw new RuntimeException("MLA default scheme is not configured");
		}
		return s;
	}

	private Schemes resolveDefaultSchemeForMlc(MLC mlc) {
		// Prefer field named "defaultScheme", fallback "scheme"
		Schemes s = tryReadSchemeField(mlc, "defaultScheme");
		if (s == null)
			s = tryReadSchemeField(mlc, "scheme");
		if (s == null || s.getId() == null) {
			throw new RuntimeException("MLC default scheme is not configured");
		}
		return s;
	}

	private Schemes tryReadSchemeField(Object owner, String fieldName) {
		try {
			var f = owner.getClass().getDeclaredField(fieldName);
			f.setAccessible(true);
			Object v = f.get(owner);
			return (v instanceof Schemes) ? (Schemes) v : null;
		} catch (NoSuchFieldException ignored) {
			return null;
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Cannot access field '" + fieldName + "' on " + owner.getClass().getSimpleName(),
					e);
		}
	}

	@Override
	public List<Schemes> searchSchemesByFilters(String username, String districtName, Long districtId,
			String schemeType) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Roles role = user.getRole();

		// ✅ For STATE roles → return all schemes (with filters)
		if (role.name().startsWith("STATE")) {
			return schemesRepository.searchSchemesWithFilters(districtId, districtName, schemeType);
		}

		// ✅ For DISTRICT/IA roles → restrict to user's district
		if (role.name().startsWith("DISTRICT") || role == Roles.IA_ADMIN) {
			if (user.getDistrict() == null)
				throw new RuntimeException("District not mapped to user");

			Long userDistrictId = user.getDistrict().getId();

			// ✅ Override districtId with user's district
			return schemesRepository.searchSchemesWithFilters(userDistrictId, null, schemeType);
		}

		throw new RuntimeException("Role not allowed to fetch schemes");
	}

	@Override
	public List<Work> getWorksBySchemeYearAndDistrict(String username, Long schemeId, String financialYear, // label or
																											// numeric
																											// id
			Long districtId) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		Roles role = user.getRole();

		// Resolve district
		Long finalDistrictId = districtId;
		if (isDistrictOrIa(role)) {
			if (user.getDistrict() == null) {
				throw new RuntimeException("District mapping missing for this user");
			}
			finalDistrictId = user.getDistrict().getId();
		} else if (isState(role)) {
			if (finalDistrictId == null) {
				throw new RuntimeException("District ID must be provided for state-level users");
			}
		}

		if (schemeId == null) {
			throw new RuntimeException("schemeId is required");
		}
		if (financialYear == null || financialYear.isBlank()) {
			throw new RuntimeException("financialYear is required");
		}

		Long financialYearId = resolveFinancialYearId(financialYear);

		return workRepository.findByScheme_IdAndFinancialYear_IdAndDistrict_Id(schemeId, financialYearId,
				finalDistrictId);
	}

	private boolean isState(Roles r) {
		return r != null && r.name().startsWith("STATE");
	}

	private boolean isDistrictOrIa(Roles r) {
		return r == Roles.DISTRICT_COLLECTOR || r == Roles.DISTRICT_DPO || r == Roles.DISTRICT_ADPO
				|| r == Roles.DISTRICT_ADMIN || r == Roles.DISTRICT_CHECKER || r == Roles.DISTRICT_MAKER
				|| r == Roles.IA_ADMIN;
	}

	/** Accepts "2025-26", "2025-2026", or a numeric string (treated as FY id). */
	private Long resolveFinancialYearId(String fy) {
		String v = normalizeFy(fy);
		// If it's all digits, treat as FY id
		if (v.matches("^\\d+$")) {
			return Long.parseLong(v);
		}
		// Else treat as the FY label and look up the entity
		return financialYearRepository.findByFinacialYearIgnoreCase(v)
				.orElseThrow(() -> new RuntimeException("Financial year not found: " + fy)).getId();
	}

	/** Normalizes short form "2025-26" to "2025-2026" for consistent lookup. */
	private String normalizeFy(String s) {
		if (s == null)
			return null;
		String t = s.trim().replace('–', '-').replace('—', '-');
		if (t.matches("^\\d{4}-\\d{2}$")) {
			int start = Integer.parseInt(t.substring(0, 4));
			int end2 = Integer.parseInt(t.substring(5, 7));
			int end = (start / 100) * 100 + end2; // 2025-26 -> 2026
			return start + "-" + end;
		}
		return t;
	}

	@Override
	public List<IdNameDto> getAllFinancialYears() {
		return financialYearRepository.findAll().stream().map(fy -> new IdNameDto(fy.getId(), fy.getFinacialYear())) // use
																														// correct
																														// field
																														// name
				.toList();
	}

	@Override
	public List<IdNameDto> getAllDivisions() {
		return divisionRepo.findAll().stream().map(d -> new IdNameDto(d.getId(), d.getDivisionName())) // replace with
																										// actual field
				.toList();
	}

	@Override
	public List<IdNameDto> getAllDistricts() {
		return districtRepo.findAll().stream().map(d -> new IdNameDto(d.getId(), d.getDistrictName())).toList();
	}

	@Override
	public List<IdNameDto> getDistrictsByDivisionId(Long divisionId) {
		return districtRepo.findByDivision_Id(divisionId).stream()
				.map(d -> new IdNameDto(d.getId(), d.getDistrictName())).toList();
	}

	@Override
	public List<IdNameDto> getTalukasByDistrictId(Long districtId) {
		return talukaRepo.findByDistrict_Id(districtId).stream().map(t -> new IdNameDto(t.getId(), t.getTalukaName()))
				.toList();
	}

	@Override
	public List<IdNameDto> getConstituenciesByDistrictId(Long districtId) {
		return constituencyRepo.findByDistrict_Id(districtId).stream()
				.map(c -> new IdNameDto(c.getId(), c.getConstituencyName())).toList();
	}

	@Override
	public List<IdNameDto> getAllSectors() {
		return sectorRepository.findAll().stream().map(s -> new IdNameDto(s.getId(), s.getSectorName()))
				.collect(Collectors.toList());
	}

	@Override
	public List<IdNameDto> getSchemesBySectorId(Long sectorId) {
		List<Schemes> schemes = schemesRepository.findBySector_Id(sectorId);
		return schemes.stream().map(s -> new IdNameDto(s.getId(), s.getSchemeName())).collect(Collectors.toList());
	}

	@Override
	public List<SchemeTypeDropdownDto> getAllSchemeTypesWithDetails() {
		List<DemandCode> schemeTypes = schemeTypeRepository.findAll();

		return schemeTypes.stream()
				.map(st -> new SchemeTypeDropdownDto(st.getId(), st.getSchemeType(), st.getBaseType().name(), // Enum as
																												// string
						st.getFinancialYear().getId(),
						st.getDistricts().stream().map(d -> d.getId()).collect(Collectors.toList())))
				.collect(Collectors.toList());
	}
}
