package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class FundTransferExecuteDto {
	private Long fundDemandId;
	private String modeOfPayment; // NEFT/RTGS/IMPS/DD
	private String debitAccountNumber; // State pool account used
	private String narration1;
	private String narration2;
}
