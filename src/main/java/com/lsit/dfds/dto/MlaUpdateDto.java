// src/main/java/com/lsit/dfds/dto/MlaUpdateDto.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class MlaUpdateDto {
	private String mlaName;
	private String party;
	private String contactNumber;
	private String email;
	private Integer term;

	// Reassign constituency if needed
	private Long constituencyId;

	// Replace full scheme list (pass all current ones to keep unchanged)
	private List<Long> schemeIds;
}
