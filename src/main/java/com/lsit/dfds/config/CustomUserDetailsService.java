package com.lsit.dfds.config;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		// Check if user is active and enabled
		if (user.getStatus() == null || 
			user.getStatus() == Statuses.INACTIVE || 
			user.getStatus() == Statuses.DESABLE || 
			user.getStatus() == Statuses.CLOSED) {
			throw new UsernameNotFoundException("User is not active or has been disabled: " + username);
		}
		
		if (user.getRole() == null) {
			throw new UsernameNotFoundException("User role not assigned: " + username);
		}

		Set<GrantedAuthority> authorities = Collections
				.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), true,
				true, true, true, authorities);
	}
}
