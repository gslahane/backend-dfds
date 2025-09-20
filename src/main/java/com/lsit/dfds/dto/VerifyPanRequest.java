// src/main/java/com/lsit/dfds/dto/VerifyPanRequest.java
package com.lsit.dfds.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyPanRequest {
	@NotBlank
	private String pan;
}
