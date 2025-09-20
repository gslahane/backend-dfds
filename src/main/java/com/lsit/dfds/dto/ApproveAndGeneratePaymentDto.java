// src/main/java/com/lsit/dfds/dto/ApproveAndGeneratePaymentDto.java
package com.lsit.dfds.dto;

import java.util.List;
import lombok.Data;

@Data
public class ApproveAndGeneratePaymentDto {
	// Select any subset for State approval
	private List<Long> fundDemandIds; // optional (ignored if approveAll=true)
	// Approve all eligible (those currently APPROVED_BY_DISTRICT)
	private Boolean approveAll; // optional, default false

	/** "NEFT"/"RTGS"/"IMPS" */
	private String modeOfPayment; // optional; default NEFT
	private String narration1; // REQUIRED (bank file)
	private String narration2; // optional
	private String filePrefix; // optional (e.g., "MMGSY")
}
