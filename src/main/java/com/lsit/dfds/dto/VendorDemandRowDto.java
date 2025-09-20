// src/main/java/com/lsit/dfds/dto/VendorDemandRowDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VendorDemandRowDto {
	private Long id;
	private String demandId;

	private Long financialYearId;
	private String financialYear;

	private Long workId;
	private String workCode;
	private String workName;

	private Long schemeId;
	private String schemeName;

	private Long districtId;
	private String districtName;

	private LocalDate demandDate;

	private Double grossAmount;
	private Double netPayable;

	private String status;

	private String paymentRefNo; // filled if transferred
	private String voucherNo; // filled if voucher exists
}
