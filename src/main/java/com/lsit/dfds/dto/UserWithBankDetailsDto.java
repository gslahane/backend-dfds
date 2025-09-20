package com.lsit.dfds.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;

import lombok.Data;

@Data
public class UserWithBankDetailsDto {
	private Long id;
	private String fullname;
	private String username;
	private Long mobile;
	private String email;
	private String designation;
	private Roles role;
	private Statuses status;
	private LocalDateTime createdAt;
	private String createdBy;
	private String districtName;
	private List<UserBankAccountDto> bankAccounts;
}
