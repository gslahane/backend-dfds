// src/main/java/com/lsit/dfds/dto/VerifyPanResponse.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VerifyPanResponse {
	private boolean ok; // true if verified
	private String code; // backend/kyc response code (e.g., SRC001)
	private String message; // human readable
	private String token; // short-lived signed token for registration
}
