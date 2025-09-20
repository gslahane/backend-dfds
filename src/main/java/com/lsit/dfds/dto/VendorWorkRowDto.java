// src/main/java/com/lsit/dfds/dto/VendorWorkRowDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendorWorkRowDto {
	private Long workId;
	private String workCode;
	private String workName;

	private String status;
	private Double adminApprovedAmount;
	private Double balanceAmount;

	private Long schemeId;
	private String schemeName;

	private Long districtId;
	private String districtName;

	private Long iaUserId; // Implementing Agency (User)
	private String iaUsername;
	private String iaFullname;
}
