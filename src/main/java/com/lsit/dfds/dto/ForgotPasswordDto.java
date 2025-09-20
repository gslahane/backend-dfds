// src/main/java/com/lsit/dfds/dto/ForgotPasswordDto.java
package com.lsit.dfds.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordDto {
	@NotBlank(message = "username is required")
	private String username;

	// at least one of email or mobile must be provided (checked in service)
	private String email; // optional
	private Long mobile; // optional

	@NotBlank(message = "newPassword is required")
	@Size(min = 8, message = "newPassword must be at least 8 characters")
	private String newPassword;

	@NotBlank(message = "confirmNewPassword is required")
	private String confirmNewPassword;
}
