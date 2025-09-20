package com.lsit.dfds.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user list responses with status and district information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserListResponseDto {

	private Long userId;
	private String fullname;
	private String username;
	private Long mobile;
	private String email;
	private String designation;
	private Roles role;
	private Statuses status;
	private Long districtId;
	private String districtName;
	private LocalDateTime createdAt;
	private String createdBy;

	// For MLA/MLC specific data
	private Long mlaId;
	private String mlaName;
	private String mlaParty;
	private Statuses mlaStatus; // MLA entity status
	private String mlaStatusChangeLetterUrl; // MLA status change letter URL

	private Long mlcId;
	private String mlcName;
	private String mlcCategory;
	private Statuses mlcStatus; // MLC entity status
	private String mlcStatusChangeLetterUrl; // MLC status change letter URL
	private Integer mlcTerm;
}