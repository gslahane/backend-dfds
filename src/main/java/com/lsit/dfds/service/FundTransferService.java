// src/main/java/com/lsit/dfds/service/FundTransferService.java
package com.lsit.dfds.service;

import java.io.IOException;

import com.lsit.dfds.dto.ApproveAndGeneratePaymentDto;
import com.lsit.dfds.dto.FundTransferExecuteDto;
import com.lsit.dfds.dto.PaymentFileGenerateDto;
import com.lsit.dfds.dto.PaymentFileResult;
import com.lsit.dfds.dto.TaxTransferExecuteDto;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.TaxDeductionFundTransfer;

public interface FundTransferService {
	FundTransfer executeVendorTransfer(FundTransferExecuteDto dto, String username); // ignores any debit in DTO

	TaxDeductionFundTransfer executeTaxTransfer(TaxTransferExecuteDto dto, String username);

	PaymentFileResult generatePaymentFileForApprovedDemands(PaymentFileGenerateDto req, String username)
			throws IOException;

	/*
	 * NEW: State approves selected/eligible demands and immediately builds &
	 * returns the payment file.
	 */
	PaymentFileResult approveAndGenerateFile(ApproveAndGeneratePaymentDto req, String username) throws IOException;

}
