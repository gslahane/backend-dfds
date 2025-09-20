// src/main/java/com/lsit/dfds/dto/ResetCredentialsDto.java
package com.lsit.dfds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetCredentialsDto {
	@NotBlank(message = "currentPassword is required")
	private String currentPassword;

	// optional: change username too
	private String newUsername;

	@NotBlank(message = "newPassword is required")
	@Size(min = 8, message = "newPassword must be at least 8 characters")
	private String newPassword;

	@NotBlank(message = "confirmNewPassword is required")
	private String confirmNewPassword;
}
