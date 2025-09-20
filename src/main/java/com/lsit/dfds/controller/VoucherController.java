// src/main/java/com/lsit/dfds/controller/VoucherController.java
package com.lsit.dfds.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.repo.FundTransferVoucherRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherController {

	private final FundTransferVoucherRepository voucherRepo;
	// If you wire a PdfService later, inject it here

	// View as HTML in browser
//	@GetMapping(value = "/{fundDemandId}/html", produces = MediaType.TEXT_HTML_VALUE)
//	public ResponseEntity<String> voucherHtml(@PathVariable Long fundDemandId) {
//		FundTransferVoucher v = voucherRepo.findByFundDemand_Id(fundDemandId)
//				.orElseThrow(() -> new RuntimeException("Voucher not found"));
//		return ResponseEntity.ok(v.getHtml());
//	}
//
//	// Convert HTML -> PDF on-demand (plug your PDF service here)
//	@GetMapping(value = "/{fundDemandId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
//	public ResponseEntity<byte[]> voucherPdf(@PathVariable Long fundDemandId) {
//		FundTransferVoucher v = voucherRepo.findByFundDemand_Id(fundDemandId)
//				.orElseThrow(() -> new RuntimeException("Voucher not found"));
//
//		// TODO: integrate PDF service. For now, return bytes from your PDF generator.
//		// byte[] pdf = pdfService.htmlToPdf(v.getHtml());
//		byte[] pdf = ("PDF generation not yet wired. Rendered HTML length: " + v.getHtml().length()).getBytes();
//
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentDisposition(ContentDisposition.inline()
//				.filename((v.getVoucherNo() != null ? v.getVoucherNo() : "voucher") + ".pdf").build());
//		return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
//	}
}
