package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class FundDemandResponseDto {

	private String demandId;
	private String schemeName;
	private String schemeType;
	private String financialYear;
	private String workName;
	private String workCode;
	private String vendorName;
	private String iaName; // Implementing Agency (from FundDemand.createdBy)

	private Double amount;
	private Double netPayable;
	private String demandStatus;
	private LocalDate demandDate;
	
	

	private String mlaName;
	private String mlcName;
	private String remarks;

	private List<TaxDetailDto> taxes;
	private int workId;
	private String planType;
}
