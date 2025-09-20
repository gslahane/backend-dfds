package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class TaxDetailDto {
	private String taxName;
	private Double taxPercentage;
	private Double taxAmount;
}
