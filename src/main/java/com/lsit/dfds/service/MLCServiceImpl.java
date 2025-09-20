package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MLCServiceImpl {

	private final MLCRepository mlcRepository;
	private final UserRepository userRepository;

	// ✅ Fetch MLCs for the logged-in District or IA user
	public List<IdNameDto> getMLCsForLoggedInUser(String username) {
		User user = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

		if (user.getDistrict() == null) {
			throw new RuntimeException("User is not mapped to any district");
		}

		Long districtId = user.getDistrict().getId();

		// ✅ Fetch both nodal MLCs (own district) and accessible MLCs
		List<MLC> nodalMLCs = mlcRepository.findByNodalDistrict(districtId);
		List<MLC> accessibleMLCs = mlcRepository.findByAccessibleDistrict(districtId);

		List<MLC> allMLCs = new ArrayList<>();
		allMLCs.addAll(nodalMLCs);
		allMLCs.addAll(accessibleMLCs);

		// ✅ Remove duplicates (if any)
		return allMLCs.stream().distinct().map(mlc -> new IdNameDto(mlc.getId(), mlc.getMlcName()))
				.collect(Collectors.toList());
	}

	// ✅ Fetch all MLCs with optional districtId filter
	public List<IdNameDto> getAllMLCs(Long districtId) {
		List<MLC> mlcs;
		if (districtId != null) {
			mlcs = mlcRepository.findAllByDistrictId(districtId);
		} else {
			mlcs = mlcRepository.findAll();
		}
		return mlcs.stream().map(mlc -> new IdNameDto(mlc.getId(), mlc.getMlcName()))
			.collect(Collectors.toList());
	}
}