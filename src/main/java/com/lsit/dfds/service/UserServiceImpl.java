package com.lsit.dfds.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.UserRequestDto;
import com.lsit.dfds.repo.UserRepository;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private UserRepository userRepository;

	@Override
	public String registerUser(UserRequestDto dto) {
		// TODO Auto-generated method stub
		return null;
	}

}
