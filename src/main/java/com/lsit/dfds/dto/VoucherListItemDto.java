// src/main/java/com/lsit/dfds/dto/VoucherListItemDto.java
package com.lsit.dfds.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoucherListItemDto {
	private Long fundDemandId;
	private String voucherNo;
	private String demandId;
	private String financialYear; // e.g. "2024-25"
	private String districtName;
	private String workName;
	private String workCode;
	private String vendorName;
	private Double grossAmount;
	private Double taxTotal;
	private Double netPayable;
	private String demandStatus; // from FundDemand.status
	private LocalDateTime createdAt;
}
