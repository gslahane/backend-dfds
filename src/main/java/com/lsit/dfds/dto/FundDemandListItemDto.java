// src/main/java/com/lsit/dfds/dto/FundDemandListItemDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDemandListItemDto {

	// Core identifiers
	private Long fundDemandId;
	private String demandId;

	// Aggregates / filters
	private String financialYear; // e.g. "2024-25"
	private String planType; // from Scheme.baseSchemeType
	private String districtName;
	private String constituencyName;

	// Work info
	private String workName;
	private String workCode;
	private Double workAaAmount; // AA amount (adminApprovedAmount)
	private Double workGrossOrderAmount; // Work gross amount

	// Vendor
	private String vendorName;

	// Demand amounts (clear separation from Work amounts)
	private Double demandGrossAmount; // d.getAmount()
	private Double demandTaxTotal; // sum of d.getTaxes()
	private List<TaxDetailDto> demandTaxDetails;
	private Double demandNetAmount; // d.getNetPayable()

	// Status & audit
	private String status;
	private LocalDate demandDate;
	private LocalDateTime createdAt;
	private String createdBy;

	// Representative
	private Long mlaId;
	private String mlaName;
	
	private Long mlcId;
	private String mlcName;
	
	private Long hadpId;
	private String hadpName;
}
