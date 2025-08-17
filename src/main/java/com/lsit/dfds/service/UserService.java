package com.lsit.dfds.service;

import com.lsit.dfds.dto.UserRegistrationDto;
import com.lsit.dfds.entity.User;

public interface UserService {

	User registerUser(UserRegistrationDto dto, String createdBy);

}
