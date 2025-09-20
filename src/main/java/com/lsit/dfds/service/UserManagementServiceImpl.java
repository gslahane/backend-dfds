package com.lsit.dfds.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.UserDto;
import com.lsit.dfds.dto.UserListResponseDto;
import com.lsit.dfds.dto.UserStatusUpdateDto;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.MLC;
import com.lsit.dfds.entity.MLCTerm;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.MLCRepository;
import com.lsit.dfds.repo.MLCTermRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

	private final UserRepository userRepository;
	private final MLARepository mlaRepository;
	private final MLCRepository mlcRepository;
	private final FileStorageService fileStorageService;
	private final MLATermRepository mlaTermRepository;
	private final MLCTermRepository mlcTermRepository;

	@Override
	@Transactional
	public GenericResponseDto<UserDto> updateUserStatus(String actorUsername, Long userId, UserStatusUpdateDto dto,
			MultipartFile letter) {
		try {
			// Get actor user
			User actor = userRepository.findByUsername(actorUsername)
					.orElseThrow(() -> new RuntimeException("Actor user not found"));

			// Get target user
			User targetUser = userRepository.findById(userId)
					.orElseThrow(() -> new RuntimeException("Target user not found"));

			// Validate authorization based on hierarchy
			validateAuthorization(actor, targetUser, dto.getStatus());

			// Handle file upload for MLA/MLC status changes
			String letterUrl = null;
			if (letter != null && !letter.isEmpty()) {
				String folder = getLetterFolder(targetUser.getRole());
				letterUrl = fileStorageService.saveFile(letter, folder);
			} else if (dto.getStatusChangeLetterUrl() != null && !dto.getStatusChangeLetterUrl().isEmpty()) {
				letterUrl = dto.getStatusChangeLetterUrl();
			}

			// Validate letter requirement for MLA/MLC
			if ((targetUser.getRole() == Roles.MLA || targetUser.getRole() == Roles.MLC)
					&& (dto.getStatus() == Statuses.INACTIVE || dto.getStatus() == Statuses.DESABLE)
					&& letterUrl == null) {
				throw new RuntimeException("Status change letter is required for MLA/MLC deactivation");
			}

			// Update user status
			targetUser.setStatus(dto.getStatus());
			userRepository.save(targetUser);

			// Update MLA/MLC status if applicable - this ensures both User and MLA/MLC
			// entities are synchronized
			if (targetUser.getRole() == Roles.MLA) {
				updateMlaStatus(targetUser, dto.getStatus(), letterUrl);
			} else if (targetUser.getRole() == Roles.MLC) {
				updateMlcStatus(targetUser, dto.getStatus(), letterUrl);
			}

			// Toggle linked user if requested (this is for additional synchronization)
			if (Boolean.TRUE.equals(dto.getAlsoToggleLinkedUser())) {
				toggleLinkedUser(targetUser, dto.getStatus());
			}

			return new GenericResponseDto<>(true, "User status updated successfully", toUserDto(targetUser));

		} catch (Exception e) {
			return new GenericResponseDto<>(false, "Failed to update user status: " + e.getMessage(), null);
		}
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getAllUsers(String actorUsername, Roles roleFilter,
			Statuses statusFilter, Long districtId) {
		try {
			User actor = userRepository.findByUsername(actorUsername)
					.orElseThrow(() -> new RuntimeException("Actor user not found"));

			List<User> users = getUsersBasedOnAuthorization(actor, roleFilter, statusFilter, districtId);
			List<UserListResponseDto> userDtos = users.stream().map(this::toUserListResponseDto)
					.collect(Collectors.toList());

			return new GenericResponseDto<>(true, "Users fetched successfully", userDtos);

		} catch (Exception e) {
			return new GenericResponseDto<>(false, "Failed to fetch users: " + e.getMessage(), null);
		}
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getUsersByRole(String actorUsername, Roles role) {
		return getAllUsers(actorUsername, role, null, null);
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getUsersByDistrict(String actorUsername, Long districtId) {
		return getAllUsers(actorUsername, null, null, districtId);
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getAllMlaUsers(String actorUsername) {
		return getUsersByRole(actorUsername, Roles.MLA);
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getAllMlcUsers(String actorUsername) {
		return getUsersByRole(actorUsername, Roles.MLC);
	}

	@Override
	public GenericResponseDto<List<UserListResponseDto>> getAllIaUsers(String actorUsername) {
		return getUsersByRole(actorUsername, Roles.IA_ADMIN);
	}

	@Override
	public GenericResponseDto<UserDto> getUserById(String actorUsername, Long userId) {
		try {
			User actor = userRepository.findByUsername(actorUsername)
					.orElseThrow(() -> new RuntimeException("Actor user not found"));

			User targetUser = userRepository.findById(userId)
					.orElseThrow(() -> new RuntimeException("Target user not found"));

			// Validate authorization
			if (!canAccessUser(actor, targetUser)) {
				throw new RuntimeException("Unauthorized to access this user");
			}

			return new GenericResponseDto<>(true, "User details fetched successfully", toUserDto(targetUser));

		} catch (Exception e) {
			return new GenericResponseDto<>(false, "Failed to fetch user details: " + e.getMessage(), null);
		}
	}

	// Helper methods

	private void validateAuthorization(User actor, User targetUser, Statuses newStatus) {
		// State users can manage all users
		if (isStateUser(actor.getRole())) {
			return;
		}

		// District users can only manage users in their district
		if (isDistrictUser(actor.getRole())) {
			if (actor.getDistrict() == null || targetUser.getDistrict() == null
					|| !actor.getDistrict().getId().equals(targetUser.getDistrict().getId())) {
				throw new RuntimeException("District users can only manage users in their district");
			}

			// District users can only manage IA_ADMIN users
			if (targetUser.getRole() != Roles.IA_ADMIN) {
				throw new RuntimeException("District users can only manage IA_ADMIN users");
			}
		}

		// Other roles cannot manage users
		throw new RuntimeException("Unauthorized to manage users");
	}

	private boolean isStateUser(Roles role) {
		return role != null && role.name().startsWith("STATE");
	}

	private boolean isDistrictUser(Roles role) {
		return role != null && role.name().startsWith("DISTRICT");
	}

	private List<User> getUsersBasedOnAuthorization(User actor, Roles roleFilter, Statuses statusFilter,
			Long districtId) {
		List<User> users;

		if (isStateUser(actor.getRole())) {
			// State users can see all users
			users = userRepository.findAll();
		} else if (isDistrictUser(actor.getRole())) {
			// District users can only see users in their district
			if (actor.getDistrict() == null) {
				users = List.of();
			} else {
				// Use the existing method with role filter as null to get all users in district
				users = userRepository.findUsersByFilter(actor.getDistrict().getId(), null, null);
			}
		} else {
			// Other roles cannot see users
			users = List.of();
		}

		// Apply filters
		return users.stream().filter(user -> roleFilter == null || user.getRole() == roleFilter)
				.filter(user -> statusFilter == null || user.getStatus() == statusFilter)
				.filter(user -> districtId == null
						|| (user.getDistrict() != null && user.getDistrict().getId().equals(districtId)))
				.collect(Collectors.toList());
	}

	private boolean canAccessUser(User actor, User targetUser) {
		if (isStateUser(actor.getRole())) {
			return true;
		}

		if (isDistrictUser(actor.getRole())) {
			return actor.getDistrict() != null && targetUser.getDistrict() != null
					&& actor.getDistrict().getId().equals(targetUser.getDistrict().getId());
		}

		return false;
	}

	private String getLetterFolder(Roles role) {
		switch (role) {
		case MLA:
			return "mla-status-letters";
		case MLC:
			return "mlc-status-letters";
		case IA_ADMIN:
			return "ia-status-letters";
		default:
			return "user-status-letters";
		}
	}

	private void updateMlaStatus(User user, Statuses status, String letterUrl) {
		MLA mla = mlaRepository.findByUser_Id(user.getId()).orElse(null);
		if (mla != null) {
			// Update MLA status to match user status
			mla.setStatus(status);
			if (letterUrl != null) {
				mla.setStatusChangeLetterUrl(letterUrl);
			}
			mlaRepository.save(mla);

			// Log the status change for audit
			System.out.println(
					"MLA status updated: MLA ID=" + mla.getId() + ", Status=" + status + ", Letter URL=" + letterUrl);
		}
	}

	private void updateMlcStatus(User user, Statuses status, String letterUrl) {
		MLC mlc = mlcRepository.findByUser_Id(user.getId()).orElse(null);
		if (mlc != null) {
			// Update MLC status to match user status
			mlc.setStatus(status);
			if (letterUrl != null) {
				mlc.setStatusChangeLetterUrl(letterUrl);
			}
			mlcRepository.save(mlc);

			// Log the status change for audit
			System.out.println(
					"MLC status updated: MLC ID=" + mlc.getId() + ", Status=" + status + ", Letter URL=" + letterUrl);
		}
	}

	private void toggleLinkedUser(User user, Statuses status) {
		if (user.getRole() == Roles.MLA) {
			MLA mla = mlaRepository.findByUser_Id(user.getId()).orElse(null);
			if (mla != null && mla.getUser() != null) {
				// Update the linked User entity status
				mla.getUser().setStatus(status);
				userRepository.save(mla.getUser());

				// Also update the MLA entity status to maintain consistency
				mla.setStatus(status);
				mlaRepository.save(mla);

				System.out.println("Linked User and MLA status synchronized: User ID=" + user.getId() + ", MLA ID="
						+ mla.getId() + ", Status=" + status);
			}
		} else if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepository.findByUser_Id(user.getId()).orElse(null);
			if (mlc != null && mlc.getUser() != null) {
				// Update the linked User entity status
				mlc.getUser().setStatus(status);
				userRepository.save(mlc.getUser());

				// Also update the MLC entity status to maintain consistency
				mlc.setStatus(status);
				mlcRepository.save(mlc);

				System.out.println("Linked User and MLC status synchronized: User ID=" + user.getId() + ", MLC ID="
						+ mlc.getId() + ", Status=" + status);
			}
		}
	}

	private UserDto toUserDto(User user) {
		UserDto dto = UserDto.builder().userId(user.getId()).fullname(user.getFullname()).username(user.getUsername())
				.mobile(user.getMobile()).email(user.getEmail()).designation(user.getDesignation()).role(user.getRole())
				.status(user.getStatus()).createdAt(user.getCreatedAt()).createdBy(user.getCreatedBy()).build();

		if (user.getDistrict() != null) {
			dto.setDistrictId(user.getDistrict().getId());
			dto.setDistrictName(user.getDistrict().getDistrictName());
		}

		// Add MLA/MLC specific data
		if (user.getRole() == Roles.MLA) {
			MLA mla = mlaRepository.findByUser_Id(user.getId()).orElse(null);
			if (mla != null) {
				dto.setMlaId(mla.getId());
				dto.setMlaName(mla.getMlaName());
				dto.setMlaParty(mla.getParty());
				dto.setMlaContactNumber(mla.getContactNumber());
				dto.setMlaStatus(mla.getStatus());
				dto.setMlaStatusChangeLetterUrl(mla.getStatusChangeLetterUrl());
				if (mla.getConstituency() != null) {
					dto.setConstituencyId(mla.getConstituency().getId());
					dto.setConstituencyName(mla.getConstituency().getConstituencyName());
				}
				// Set MLA term number from MLATerm
				MLATerm term = mlaTermRepository.findFirstByMla_IdAndActiveTrue(mla.getId()).orElse(null);
				if (term != null) {
					dto.setMlaTerm(term.getTermNumber());
				}
			}
		} else if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepository.findByUser_Id(user.getId()).orElse(null);
			if (mlc != null) {
				dto.setMlcId(mlc.getId());
				dto.setMlcName(mlc.getMlcName());
				dto.setMlcCategory(mlc.getCategory());
				dto.setMlcContactNumber(mlc.getContactNumber());
				dto.setMlcRegion(mlc.getRegion());
				dto.setMlcStatus(mlc.getStatus()); // Include MLC entity status
				dto.setMlcStatusChangeLetterUrl(mlc.getStatusChangeLetterUrl()); // Include status change letter URL
				// Set MLC term number from MLCTerm
				MLCTerm term = mlcTermRepository.findFirstByMlc_IdAndActiveTrue(mlc.getId()).orElse(null);
				if (term != null) {
					dto.setMlcTerm(term.getTermNumber());
				}
			}
		}

		return dto;
	}

	private UserListResponseDto toUserListResponseDto(User user) {
		UserListResponseDto dto = UserListResponseDto.builder().userId(user.getId()).fullname(user.getFullname())
				.username(user.getUsername()).mobile(user.getMobile()).email(user.getEmail())
				.designation(user.getDesignation()).role(user.getRole()).status(user.getStatus())
				.createdAt(user.getCreatedAt()).createdBy(user.getCreatedBy()).build();

		if (user.getDistrict() != null) {
			dto.setDistrictId(user.getDistrict().getId());
			dto.setDistrictName(user.getDistrict().getDistrictName());
		}

		// Add MLA/MLC specific data
		if (user.getRole() == Roles.MLA) {
			MLA mla = mlaRepository.findByUser_Id(user.getId()).orElse(null);
			if (mla != null) {
				dto.setMlaId(mla.getId());
				dto.setMlaName(mla.getMlaName());
				dto.setMlaParty(mla.getParty());
				dto.setMlaStatus(mla.getStatus()); // Include MLA entity status
				dto.setMlaStatusChangeLetterUrl(mla.getStatusChangeLetterUrl()); // Include status change letter URL
			}
		} else if (user.getRole() == Roles.MLC) {
			MLC mlc = mlcRepository.findByUser_Id(user.getId()).orElse(null);
			if (mlc != null) {
				dto.setMlcId(mlc.getId());
				dto.setMlcName(mlc.getMlcName());
				dto.setMlcCategory(mlc.getCategory());
				dto.setMlcStatus(mlc.getStatus()); // Include MLC entity status
				dto.setMlcStatusChangeLetterUrl(mlc.getStatusChangeLetterUrl()); // Include status change letter URL
				// Set MLC term number from MLCTerm
				MLCTerm term = mlcTermRepository.findFirstByMlc_IdAndActiveTrue(mlc.getId()).orElse(null);
				if (term != null) {
					dto.setMlcTerm(term.getTermNumber());
				}
			}
		}

		return dto;
	}
}