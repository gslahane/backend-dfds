package com.lsit.dfds.dto;

import com.lsit.dfds.enums.Statuses;

import lombok.Data;

@Data
public class UserBankAccountDto {
	private String accountHolderName;
	private String accountNumber;
	private String ifsc;
	private String bankName;
	private String branch;
	private String accountType;
	private Boolean isPrimary;
	private Boolean verified;
	private Statuses status;
}
