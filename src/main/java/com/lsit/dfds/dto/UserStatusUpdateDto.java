package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.Statuses;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user status updates (activate/deactivate/enable/disable)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserStatusUpdateDto {

	@NotNull(message = "status is required")
	private Statuses status; // ACTIVE, INACTIVE, ENABLE, DESABLE

	private String reason;

	// Optional: if no file is uploaded, you may supply a pre-existing URL
	private String statusChangeLetterUrl;

	// Optional: for MLA/MLC, also toggle their linked user status
	private Boolean alsoToggleLinkedUser;
}
