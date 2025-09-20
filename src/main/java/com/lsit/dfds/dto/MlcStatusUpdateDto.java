package com.lsit.dfds.dto;

//src/main/java/com/lsit/dfds/dto/MlcStatusUpdateDto.java

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MlcStatusUpdateDto {
	// Optional: if no file uploaded, you can supply a pre-existing link
	private String statusChangeLetterUrl;

	// Optional: human-readable reason
	private String reason;

	// Optional: toggle linked user status in sync
	private Boolean alsoToggleUser;
}
