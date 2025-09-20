// src/main/java/com/lsit/dfds/dto/MlaInfoDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class MlaInfoDto {
	private Long id;
	private String mlaName;
	private String party;
	private String contactNumber;
	private String email;
	private Integer termNumber;
	private LocalDate tenureStartDate;
	private LocalDate tenureEndDate;
	private String tenurePeriod;
	private String status;

	private Long constituencyId;
	private String constituencyName;
	private Long districtId;
	private String districtName;

	private List<Long> schemeIds;
	private List<String> schemeNames;

	// Linked user (optional)
	private Long userId;
	private String username;
}