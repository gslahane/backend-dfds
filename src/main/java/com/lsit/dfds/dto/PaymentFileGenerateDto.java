// src/main/java/com/lsit/dfds/dto/PaymentFileGenerateDto.java
package com.lsit.dfds.dto;

import java.util.List;
import lombok.Data;

@Data
public class PaymentFileGenerateDto {
	private List<Long> fundDemandIds; // required (non-empty)
	/** "NEFT"/"RTGS"/"IMPS" */
	private String modeOfPayment; // optional; default NEFT
	private String narration1; // required
	private String narration2; // optional
	private String filePrefix; // optional (e.g., "MMGSY")
}
