package com.lsit.dfds.dto;

import com.lsit.dfds.enums.Roles;

import lombok.Data;

@Data
public class UserFilterDto {
	private Long districtId;
	private Roles role;
	private String search; // Universal search by username, fullname, or designation
}
