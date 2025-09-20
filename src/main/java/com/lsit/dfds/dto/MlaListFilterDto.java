// src/main/java/com/lsit/dfds/dto/MlaListFilterDto.java
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class MlaListFilterDto {
	private Long districtId;
	private Long constituencyId;
	private String status; // ACTIVE / INACTIVE, etc. from Statuses
	private String search; // name/email/phone like
}
