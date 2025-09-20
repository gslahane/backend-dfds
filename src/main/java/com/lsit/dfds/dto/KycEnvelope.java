// src/main/java/com/lsit/dfds/dto/KycEnvelope.java
package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KycEnvelope {
	private Object data;
	private String referenceId;
	private String responseMessage;
	private String responseCode; // "SRC001" => success

	public boolean isOk() {
		return responseCode != null && responseCode.trim().equalsIgnoreCase("SRC001");
	}
}
