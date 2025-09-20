package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import com.lsit.dfds.enums.Roles;

import lombok.Data;

//--- Registration DTO (single payload for all roles) ---
//com.lsit.dfds.dto.UserRegistrationDto
@Data
public class UserRegistrationDto {
	private String fullname;
	private String username;
	private String email;
	private String designation;
	private String password;
	private Long mobile;
	private String agencyCode; // keep if you use it on UI/badges
	private Roles role;

// District scoping for district/IA/HADP roles
	private Long districtId;

// MLA creation/linking
	private Long existingMlaId;
	private String mlaName;
	private String mlaParty;
	private String mlaContactNumber;
	private String mlaEmail;
	private Integer mlaTerm;
	private Long constituencyId;
	// MLA tenure dates
	private LocalDate mlaTenureStartDate;
	private LocalDate mlaTenureEndDate;

// MLC creation/linking
	private Long existingMlcId;
	private String mlcName;
	private String mlcCategory;
	private String mlcContactNumber;
	private String mlcEmail;
	private Integer mlcTerm;
	private String mlcRegion;
	private List<Long> accessibleDistrictIds;
	private List<Long> accessibleConstituencyIds;
	// MLC tenure dates
	private LocalDate mlcTenureStartDate;
	private LocalDate mlcTenureEndDate;

// HADP scoping (optional)
	private Long talukaId;

// MLA representative
	private Long repForMlaId;

// Optional initial bank for internal users (stored in UserBankAccount)
	private String bankAccountNumber;
	private String bankIfsc;
	private String bankName;
	private String bankBranch;
	private String accountType; // CA/SB
}