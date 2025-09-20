package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for work approval requests (ADPO, DPO, Collector approvals)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkApprovalDto {

	@NotNull(message = "workId is required")
	private Long workId;

	@NotNull(message = "approvalStatus is required")
	private String approvalStatus; // APPROVED, REJECTED

	private String remarks;

	private LocalDate approvalDate;

	// Optional: new amount if budget is being adjusted
	private Double adjustedAmount;

	// Optional: implementing agency assignment
	private Long implementingAgencyUserId;

	// Optional: vendor assignment
	private Long vendorId;

	/**
	 * Optional: if no file is uploaded, you may supply a pre-existing URL to the
	 * approval letter. If a file is uploaded, it takes precedence.
	 */
	private String approvalLetterUrl;

	private List<TaxDetailDto> taxes; // optional (frontend-calculated tax lines)

}
