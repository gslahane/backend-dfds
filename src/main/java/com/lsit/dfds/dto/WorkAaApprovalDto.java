// src/main/java/com/lsit/dfds/dto/WorkAaApprovalDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent as JSON in the "payload" part of multipart/form-data for AA approval. If
 * a file "aaLetter" is uploaded, it takes precedence over aaLetterUrl.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkAaApprovalDto {

	@NotNull(message = "workId is required")
	private Long workId;

	@Positive(message = "adminApprovedAmount must be > 0")
	private Double adminApprovedAmount;

	// optional (defaults to today in service)
	private LocalDate aaApprovalDate;

	// Optional: assign IA (must be in same district and role=IA_ADMIN)
	private Long implementingAgencyUserId;
	
	private String workCode;
	
	
	// Optional: immediate vendor map
//	private Long vendorId;

	/**
	 * Optional: if no file is uploaded, you may supply a pre-existing URL to the AA
	 * approval letter. If a file is uploaded, it takes precedence.
	 */
	private String aaLetterUrl;

	private List<TaxDetailDto> taxes; // optional (frontend-calculated tax lines)

}
