package com.lsit.dfds.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.Division;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Taluka;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.DivisionRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.TalukaRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DropdownServiceImpl implements DropdownService {

	private final UserRepository userRepository;
	private final SchemesRepository schemesRepository;

	@Autowired
	private DivisionRepository divisionRepo;

	@Autowired
	private DistrictRepository districtRepo;

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
	public List<Division> getAllDivisions() {
		return divisionRepo.findAll();
	}

	@Override
	public List<District> getAllDistricts() {
		return districtRepo.findAll();
	}

	@Override
	public List<District> getDistrictsByDivisionId(Long divisionId) {
		return districtRepo.findByDivision_Id(divisionId);
	}

	@Override
	public List<Taluka> getTalukasByDistrictId(Long districtId) {
		return talukaRepo.findByDistrict_Id(districtId);
	}

	@Override
	public List<Constituency> getConstituenciesByDistrictId(Long districtId) {
		return constituencyRepo.findByDistrict_Id(districtId);
	}
}
