package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MLAServiceImpl {

	private final MLARepository mlaRepository;

	private final UserRepository userRepository;

	// ✅ Fetch all active MLAs or all MLAs
	public List<IdNameDto> getAllMLANames(boolean onlyActive) {
		List<MLA> mlas = onlyActive ? mlaRepository.findByStatus(Statuses.ACTIVE) : mlaRepository.findAll();

		return mlas.stream().map(mla -> new IdNameDto(mla.getId(), mla.getMlaName())).collect(Collectors.toList());
	}

	// ✅ Fetch MLAs by Constituency ID
	public List<IdNameDto> getMLAsByConstituency(Long constituencyId) {
		List<MLA> mlas = mlaRepository.findByConstituency_Id(constituencyId);

		return mlas.stream().map(mla -> new IdNameDto(mla.getId(), mla.getMlaName())).collect(Collectors.toList());
	}

	// ✅ Fetch MLA list for logged-in District or IA user
	public List<IdNameDto> getMLAsForLoggedInUser(String username) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getDistrict() == null) {
			throw new RuntimeException("User is not mapped to any district");
		}

		Long districtId = user.getDistrict().getId();

		List<MLA> mlas = mlaRepository.findByDistrictId(districtId);

		return mlas.stream().map(mla -> new IdNameDto(mla.getId(), mla.getMlaName())).collect(Collectors.toList());
	}

	public List<IdNameDto> getMlaDropdownByDistrict(Long districtId) {
		List<MLA> mlas = mlaRepository.findByConstituency_District_Id(districtId);
		return mlas.stream().map(m -> new IdNameDto(m.getId(), m.getMlaName())).collect(Collectors.toList()); // use
																												// toList()
																												// only
																												// if
																												// you're
																												// on
																												// Java
																												// 16+
	}
}
