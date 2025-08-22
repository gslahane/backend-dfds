package com.lsit.dfds.service;

import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.entity.User;

public interface UserService {

	// com.lsit.dfds.service.UserService
	User registerDistrictUser(UserRegistrationDto dto, String createdBy); // new helper for district staff

	User registerUser(UserRegistrationDto dto, String createdBy); // existing core

}
