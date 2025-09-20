package com.lsit.dfds.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserListResponseDto;
import com.lsit.dfds.dto.UserStatusUpdateDto;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;

/**
 * Service interface for comprehensive user management
 */
public interface UserManagementService {

	/**
	 * Update user status (activate/deactivate/enable/disable)
	 */
	GenericResponseDto<UserDto> updateUserStatus(String actorUsername, Long userId, UserStatusUpdateDto dto,
			MultipartFile letter);

	/**
	 * Get all users based on actor's role and permissions
	 */
	GenericResponseDto<List<UserListResponseDto>> getAllUsers(String actorUsername, Roles roleFilter,
			Statuses statusFilter, Long districtId);

	/**
	 * Get users by role with proper authorization
	 */
	GenericResponseDto<List<UserListResponseDto>> getUsersByRole(String actorUsername, Roles role);

	/**
	 * Get users by district with proper authorization
	 */
	GenericResponseDto<List<UserListResponseDto>> getUsersByDistrict(String actorUsername, Long districtId);

	/**
	 * Get all MLA users with their status and district information
	 */
	GenericResponseDto<List<UserListResponseDto>> getAllMlaUsers(String actorUsername);

	/**
	 * Get all MLC users with their status and district information
	 */
	GenericResponseDto<List<UserListResponseDto>> getAllMlcUsers(String actorUsername);

	/**
	 * Get all IA users with their status and district information
	 */
	GenericResponseDto<List<UserListResponseDto>> getAllIaUsers(String actorUsername);

	/**
	 * Get user details by ID with proper authorization
	 */
	GenericResponseDto<UserDto> getUserById(String actorUsername, Long userId);
}
