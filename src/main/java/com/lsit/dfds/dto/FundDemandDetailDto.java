// FundDemandDetailDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundDemandDetailDto {
	private Long id;
	private String demandId;
	private Long financialYearId;
	private String financialYear;
	private Long workId;
	private String workName;
	private String workCode;
	private Long vendorId;
	private String vendorName;
	private Double amount;
	private Double netPayable;
	private LocalDate demandDate;
	private String status;
	private Double adminApprovedAmount;
	private Double workPortionAmount;
}
