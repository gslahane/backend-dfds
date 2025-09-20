// TaxTransferExecuteDto.java
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class TaxTransferExecuteDto {
	private Long fundDemandId;
	private String debitAccountNumber; // State tax-deduction pool account
	private String remarks;
}