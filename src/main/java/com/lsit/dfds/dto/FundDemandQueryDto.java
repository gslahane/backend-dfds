// src/main/java/com/lsit/dfds/dto/FundDemandQueryDto.java
package com.lsit.dfds.dto;

import com.lsit.dfds.enums.DemandStatuses;

import lombok.Data;

@Data
public class FundDemandQueryDto {
	private Long financialYearId;
	private Long districtId; // State may pass; District ignored (forced by role)
	private DemandStatuses status; // Optional single status
	private String q; // search (demandId, vendorName, workName/code)
}
