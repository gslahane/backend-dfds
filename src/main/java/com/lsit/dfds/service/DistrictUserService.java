package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserFilterDto;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistrictUserService {
	private final UserRepository userRepository;

	public List<UserDto> getUsers(UserFilterDto filter) {
		List<User> users;

		// If filter is null or the fields inside filter are all null (i.e., no filter),
		// fetch all district role users
		if (filter == null
				|| (filter.getDistrictId() == null && filter.getRole() == null && filter.getSearch() == null)) {
			// If no filter is provided, fetch all users with district roles
			List<Roles> districtRoles = List.of(Roles.DISTRICT_COLLECTOR, Roles.DISTRICT_DPO, Roles.DISTRICT_ADPO,
					Roles.DISTRICT_ADMIN, Roles.DISTRICT_CHECKER, Roles.DISTRICT_MAKER);

			// Fetch users by district roles and apply optional filters (districtId, search)
			// if provided
			users = userRepository.findUsersByRoles(districtRoles, null, null); // pass null for non-filtered
		} else {
			// If filters are provided, use the filter method
			users = userRepository.findUsersByFilter(filter.getDistrictId(), filter.getRole(), filter.getSearch());
		}

		// Convert the User entities to UserDto
		return users.stream().map(this::toUserDto).collect(Collectors.toList());
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

		return dto;
	}

	public void updateUserStatus(Long userId, Statuses status) {
		User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
		user.setStatus(status);
		userRepository.save(user);
	}
}
