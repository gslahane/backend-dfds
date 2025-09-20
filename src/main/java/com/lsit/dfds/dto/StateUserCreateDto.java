// src/main/java/com/lsit/dfds/dto/StateUserCreateDto.java
package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.Roles;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StateUserCreateDto {

	@NotBlank(message = "fullname is required")
	@Size(max = 255)
	private String fullname;

	@NotBlank(message = "username is required")
	@Size(max = 100)
	private String username;

	@Email(message = "email is invalid")
	private String email;

	@Size(max = 120)
	private String designation;

	@NotBlank(message = "password is required")
	@Size(min = 6, max = 100, message = "password length must be 6-100")
	private String password;

	// Optional (stored as Long in User)
	@Pattern(regexp = "^\\d{10}$", message = "mobile must be 10 digits")
	private String mobile;

	@NotNull(message = "role is required")
	private Roles role; // STATE_ADMIN, STATE_CHECKER, STATE_MAKER

	// Optional: chain-of-command inside state
	private Long supervisorUserId; // must be another STATE_* user if provided

	// ---------- Optional initial bank details (persisted to UserBankAccount)
	// ----------
	// Keep the validation light & optional; enforce only if provided
	@Pattern(regexp = "^[0-9A-Za-z\\-\\s]{6,22}$", message = "bankAccountNumber looks invalid")
	private String bankAccountNumber;

	@Pattern(regexp = "^[A-Za-z]{4}0[A-Za-z0-9]{6}$", message = "bankIfsc is invalid")
	private String bankIfsc;

	@Size(max = 120, message = "bankName is too long")
	private String bankName;

	@Size(max = 120, message = "bankBranch is too long")
	private String bankBranch;

	// e.g. "SB" / "CA"
	@Pattern(regexp = "^(SB|CA)$", message = "accountType must be SB or CA")
	private String accountType;
}
