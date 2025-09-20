package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.StateUserCreateDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.dto.UserWithBankDetailsDto;
import com.lsit.dfds.entity.User;

public interface UserService {

	// com.lsit.dfds.service.UserService
	User registerDistrictUser(UserRegistrationDto dto, String createdBy); // new helper for district staff

	User registerUser(UserRegistrationDto dto, String createdBy); // existing core

	UserDto registerStateUser(StateUserCreateDto dto, String createdBy);

	List<UserWithBankDetailsDto> getIAUsersWithBankDetails();

}
