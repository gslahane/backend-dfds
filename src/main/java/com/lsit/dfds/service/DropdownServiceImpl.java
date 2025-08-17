package com.lsit.dfds.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.SchemeTypeDropdownDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.DivisionRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
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

	@Override
	public List<String> getSchemeNamesForUser(String username) {
		Optional<User> userOpt = userRepository.findByUsername(username);

		if (userOpt.isEmpty()) {
			throw new RuntimeException("User not found: " + username);
		}

		User user = userOpt.get();
		Roles role = user.getRole();

		if (role == Roles.STATE_ADMIN) {
			return schemesRepository.findAll().stream().map(Schemes::getSchemeName).distinct()
					.collect(Collectors.toList());

		} else if (role == Roles.DISTRICT_ADMIN || role == Roles.IA_ADMIN) {
			if (user.getDistrict() == null) {
				throw new RuntimeException("District not mapped for user: " + username);
			}

			Long districtId = user.getDistrict().getId();

			return schemesRepository.findByDistricts_Id(districtId).stream().map(Schemes::getSchemeName).distinct()
					.collect(Collectors.toList());

		} else {
			throw new RuntimeException("Unsupported role for dropdown: " + role);
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
	public List<Work> getWorksBySchemeYearAndDistrict(String username, Long schemeId, String financialYear,
			Long districtId) {
		Optional<User> userOpt = userRepository.findByUsername(username);
		if (userOpt.isEmpty()) {
			throw new RuntimeException("User not found");
		}

		User user = userOpt.get();
		Roles role = user.getRole();

		Long finalDistrictId = districtId;

		// ✅ If user is District Admin or IA Admin, take district from user (ignore
		// frontend)
		if (role == Roles.DISTRICT_ADMIN || role == Roles.DISTRICT_MAKER || role == Roles.DISTRICT_CHECKER
				|| role == Roles.IA_ADMIN) {

			if (user.getSupervisor() == null || user.getSupervisor().getSupervisor() == null) {
				throw new RuntimeException("District mapping missing for this user");
			}

			// Fetch districtId from user hierarchy (or directly from user if User has
			// district)
			finalDistrictId = user.getDistrict().getId();
		}

		// ✅ For State roles, use districtId from frontend (must be provided)
		if ((role == Roles.STATE_ADMIN || role == Roles.STATE_CHECKER || role == Roles.STATE_MAKER)
				&& finalDistrictId == null) {
			throw new RuntimeException("District ID must be provided for state-level users");
		}

		return workRepository.findByScheme_IdAndFinancialYearAndScheme_Districts_Id(schemeId, financialYear,
				finalDistrictId);
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
