// src/main/java/com/lsit/dfds/dto/MlaStatusUpdateDto.java
package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlaStatusUpdateDto {
	@NotBlank(message = "status is required") // e.g., ACTIVE / INACTIVE
	private String status;

	// Optional: if no file uploaded, you can supply a pre-existing link
	private String statusChangeLetterUrl;

	// Optional: human-readable reason
	private String reason;

	// optional; default false
	private Boolean alsoToggleUser;
}
