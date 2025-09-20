package com.lsit.dfds.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.PaymentFileResult;
import com.lsit.dfds.utils.H2HSftpClient;
import com.lsit.dfds.utils.PaymentFileBuilder;
import com.lsit.dfds.utils.PaymentFileBuilder.PaymentRow;

@RestController
@RequestMapping("/api/h2h/dev")
public class H2HDevController {

	private final PaymentFileBuilder builder;
	private final H2HSftpClient sftp;

	public H2HDevController(PaymentFileBuilder builder, @Qualifier("bankSftpClient") H2HSftpClient sftp) {
		this.builder = builder;
		this.sftp = sftp;
	}

	@PostMapping("/generate-and-upload-dummy")
	public ResponseEntity<?> dummy(@RequestParam(defaultValue = "NEFT") String mode,
			@RequestParam(defaultValue = "PAYTEST") String prefix) {
		try {
			PaymentRow r1 = new PaymentRow();
			r1.debitAccountNo = "1234567890123456";
			r1.modeCode = mode;
			r1.beneficiaryAccountNo = "111122223333";
			r1.beneficiaryName = "ABC VENDOR PVT LTD";
			r1.amount = 1000.00;
			r1.add1 = "Line 1";
			r1.add2 = "Pune";
			r1.add3 = "MH";
			r1.pincode = "411001";
			r1.mobile = "9000000000";
			r1.email = "vendor@example.com";
			r1.ddPayableAt = "NA";
			r1.ifsc = "MAHB0000008";
			r1.branch = "Shivajinagar";
			r1.bank = "Bank of Maharashtra";
			r1.accountType = "CA";
			r1.narration1 = "DFDS TEST";
			r1.narration2 = "Test run";
			r1.paymentRefNo = "PAY-DEV-1";

			PaymentRow r2 = new PaymentRow();
			r2.debitAccountNo = "1234567890123456";
			r2.modeCode = "IMPS";
			r2.beneficiaryAccountNo = "444455556666";
			r2.beneficiaryName = "DISTRICT IA PRIMARY";
			r2.amount = 250.50;
			r2.add1 = "IA";
			r2.add2 = "Nashik";
			r2.add3 = "MH";
			r2.pincode = "422001";
			r2.mobile = "9000000001";
			r2.email = "ia@example.com";
			r2.ddPayableAt = "NA";
			r2.ifsc = "MAHB0000007";
			r2.branch = "Main";
			r2.bank = "Bank of Maharashtra";
			r2.accountType = "SB";
			r2.narration1 = "TAX-DFDS TEST";
			r2.narration2 = "Tax";
			r2.paymentRefNo = "PAY-DEV-2";

			PaymentFileResult res = builder.writePaymentFile(List.of(r1, r2), prefix);

			sftp.upload(res.getAbsolutePath(), res.getFileName());

			return ResponseEntity.ok(Map.of("fileName", res.getFileName(), "absolutePath", res.getAbsolutePath(),
					"totalRows", res.getTotalRows(), "totalAmount", res.getTotalAmount(), "message",
					"Dummy file generated and uploaded to SFTP IN folder."));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
