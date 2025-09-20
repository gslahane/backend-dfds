package com.lsit.dfds.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.ApproveAndGeneratePaymentDto;
import com.lsit.dfds.dto.FundTransferExecuteDto;
import com.lsit.dfds.dto.PaymentFileGenerateDto;
import com.lsit.dfds.dto.PaymentFileResult;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.service.FundTransferService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fund-transfers")
@RequiredArgsConstructor
public class FundTransferController {

	private final FundTransferService fundTransferService;
	private final JwtUtil jwtUtil;

	@PostMapping("/state/transfer-one")
	public ResponseEntity<?> transferOne(@RequestHeader("Authorization") String auth,
			@RequestBody FundTransferExecuteDto dto) {
		try {
			String username = jwtUtil.extractUsername(auth.substring(7));
			FundTransfer ft = fundTransferService.executeVendorTransfer(dto, username);
			return ResponseEntity.ok(ft);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/state/generate-file")
	public ResponseEntity<?> generateFile(@RequestHeader("Authorization") String auth,
			@RequestBody PaymentFileGenerateDto req) {
		try {
			String username = jwtUtil.extractUsername(auth.substring(7));
			PaymentFileResult result = fundTransferService.generatePaymentFileForApprovedDemands(req, username);
			return ResponseEntity.ok(Map.of("fileName", result.getFileName(), "absolutePath", result.getAbsolutePath(),
					"totalRows", result.getTotalRows(), "totalAmount", result.getTotalAmount(), "includedIds",
					result.getDemandIds(), "message",
					"Payment file uploaded to bank SFTP. Please authorize in Mahapay (OTP)."));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/state/approve-and-generate-file")
	public ResponseEntity<?> approveAndGenerate(@RequestHeader("Authorization") String auth,
			@RequestBody ApproveAndGeneratePaymentDto req) {
		try {
			String username = jwtUtil.extractUsername(auth.substring(7));
			PaymentFileResult result = fundTransferService.approveAndGenerateFile(req, username);
			return ResponseEntity.ok(Map.of("fileName", result.getFileName(), "absolutePath", result.getAbsolutePath(),
					"totalRows", result.getTotalRows(), "totalAmount", result.getTotalAmount(), "includedIds",
					result.getDemandIds(), "message",
					"Payment file uploaded to bank SFTP. Please authorize in Mahapay (OTP)."));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
		}
	}
}
