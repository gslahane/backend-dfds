package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class TaxRequestDto {
	private String code;
	private String taxName;
	private String taxType; // PERCENTAGE or FIXED
	private Double taxPercentage;
	private Double fixedAmount;
	private String remarks;
}
