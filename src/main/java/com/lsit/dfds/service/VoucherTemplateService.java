// src/main/java/com/lsit/dfds/service/VoucherTemplateService.java
package com.lsit.dfds.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.FundTransferVoucher; // <-- recommended entity holding voucherNo
import com.lsit.dfds.entity.TaxDeductionFundTransfer;

@Service
public class VoucherTemplateService {

	public String renderHtml(FundDemand d, FundTransfer ft, TaxDeductionFundTransfer tax, FundTransferVoucher vch) {
		// Voucher number strategy: prefer voucher entity; fallback to paymentRefNo
		String voucherNo = vch != null && vch.getVoucherNo() != null ? vch.getVoucherNo()
				: (ft != null && ft.getPaymentRefNo() != null ? ft.getPaymentRefNo() : "");

		String taxRows = (tax != null && tax.getTaxes() != null && !tax.getTaxes().isEmpty())
				? tax.getTaxes().stream()
						.map(t -> "<tr>" + "<td>" + esc(t.getTaxName()) + "</td>" + "<td class='right'>"
								+ safePct(t.getTaxPercentage()) + "</td>" + "<td class='right'>"
								+ safeAmt(t.getTaxAmount()) + "</td>" + "</tr>")
						.reduce("", (a, b) -> a + b)
				: "<tr><td colspan='3' class='muted' style='text-align:center;'>No taxes</td></tr>";

		double taxTotal = (tax != null && tax.getTaxes() != null)
				? tax.getTaxes().stream().mapToDouble(t -> t.getTaxAmount() == null ? 0.0 : t.getTaxAmount()).sum()
				: 0.0;

		String schemeName = (d.getWork() != null && d.getWork().getScheme() != null)
				? d.getWork().getScheme().getSchemeName()
				: "";
		String workName = d.getWork() != null ? d.getWork().getWorkName() : "";
		String workCode = d.getWork() != null ? d.getWork().getWorkCode() : "";

		String vendorName = d.getVendor() != null ? d.getVendor().getName() : "";
		String vendorAcc = d.getVendor() != null ? d.getVendor().getBankAccountNumber() : "";
		String vendorIfsc = d.getVendor() != null ? d.getVendor().getBankIfsc() : "";
		String vendorBank = d.getVendor() != null ? d.getVendor().getBankName() : "";
		String vendorEmail = d.getVendor() != null ? nz(d.getVendor().getEmail()) : "";
		String vendorPhone = d.getVendor() != null ? nz(d.getVendor().getPhone()) : "";

		String paymentMode = ft != null ? nz(ft.getModeOfPayment()) : "";
		String debitAcc = ft != null ? nz(ft.getDebitAccountNumber()) : "";
		String beneType = ft != null ? nz(ft.getBeneficiaryAccountType()) : "";
		String paymentRef = ft != null ? nz(ft.getPaymentRefNo()) : "";

		String demandDate = fmtDate(d.getDemandDate());
		String transferDate = ft != null ? fmtDateTime(ft.getCreatedAt()) : "";

		return """
				<html>
				  <head>
				    <meta charset="utf-8" />
				    <title>Payment Voucher</title>
				    <style>
				      * { box-sizing: border-box; }
				      body { font-family: Arial, sans-serif; font-size: 12px; color: #222; margin: 20px; }
				      .header {
				        display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px;
				        border-bottom: 2px solid #0b6; padding-bottom: 8px;
				      }
				      .brand { font-size: 16px; font-weight: bold; color: #0b6; }
				      .sub { color: #666; font-size: 11px; }
				      h2 { margin: 12px 0 8px 0; font-size: 16px; }
				      h3 { margin: 12px 0 6px 0; font-size: 13px; color: #0b6; }
				      table { width: 100%%; border-collapse: collapse; margin-top: 6px; }
				      th, td { border: 1px solid #d4d4d4; padding: 6px; vertical-align: top; }
				      th { background: #f6fdf9; text-align: left; }
				      .right { text-align: right; }
				      .muted { color: #777; }
				      .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
				      .card { border: 1px solid #e8e8e8; border-radius: 6px; padding: 10px; background: #fff; }
				      .meta-table td { border: none; padding: 2px 0; }
				      .footer-note { margin-top: 18px; font-size: 11px; color: #666; }
				      .sign-row { margin-top: 24px; display:flex; gap: 20px; }
				      .sign { flex: 1; text-align: center; }
				      .sign .line { margin-top: 40px; border-top: 1px solid #888; padding-top: 4px; color:#555; font-size: 11px; }
				    </style>
				  </head>
				  <body>
				    <div class="header">
				      <div class="brand">Payment Voucher</div>
				      <div class="sub">Auto-generated document</div>
				    </div>

				    <div class="grid">
				      <div class="card">
				        <h3>Voucher & Demand</h3>
				        <table class="meta-table">
				          <tr><td><b>Voucher No</b></td><td>: %s</td></tr>
				          <tr><td><b>Payment Ref</b></td><td>: %s</td></tr>
				          <tr><td><b>Demand ID</b></td><td>: %s</td></tr>
				          <tr><td><b>Demand Date</b></td><td>: %s</td></tr>
				          <tr><td><b>Transfer Date</b></td><td>: %s</td></tr>
				        </table>
				      </div>

				      <div class="card">
				        <h3>Payment Info</h3>
				        <table class="meta-table">
				          <tr><td><b>Mode</b></td><td>: %s</td></tr>
				          <tr><td><b>Debit A/C</b></td><td>: %s</td></tr>
				          <tr><td><b>Beneficiary Type</b></td><td>: %s</td></tr>
				        </table>
				      </div>
				    </div>

				    <h3>Work / Scheme</h3>
				    <table>
				      <tr><th>Scheme</th><th>Work</th><th>Work Code</th></tr>
				      <tr>
				        <td>%s</td>
				        <td>%s</td>
				        <td>%s</td>
				      </tr>
				    </table>

				    <h3>Vendor</h3>
				    <table>
				      <tr><th>Name</th><th>Account</th><th>IFSC</th><th>Bank</th><th>Email</th><th>Phone</th></tr>
				      <tr>
				        <td>%s</td>
				        <td>%s</td>
				        <td>%s</td>
				        <td>%s</td>
				        <td>%s</td>
				        <td>%s</td>
				      </tr>
				    </table>

				    <h3>Amounts</h3>
				    <table>
				      <tr><th>Gross Amount</th><th>Tax Total</th><th>Net Payable</th></tr>
				      <tr>
				        <td class="right">%s</td>
				        <td class="right">%s</td>
				        <td class="right"><b>%s</b></td>
				      </tr>
				    </table>

				    <h3>Tax Breakup</h3>
				    <table>
				      <tr><th>Tax</th><th class="right">Rate</th><th class="right">Amount</th></tr>
				      %s
				    </table>

				    <div class="footer-note">
				      This voucher is generated based on authorized fund transfer. Please verify details before use.
				    </div>

				    <div class="sign-row">
				      <div class="sign">
				        <div class="line">Authorized Signatory</div>
				      </div>
				      <div class="sign">
				        <div class="line">Accountant / Treasurer</div>
				      </div>
				    </div>
				  </body>
				</html>
				"""
				.formatted(esc(voucherNo), esc(paymentRef), esc(d.getDemandId()), esc(demandDate), esc(transferDate),

						esc(paymentMode), esc(debitAcc), esc(beneType),

						esc(schemeName), esc(workName), esc(workCode),

						esc(vendorName), esc(vendorAcc), esc(vendorIfsc), esc(vendorBank), esc(vendorEmail),
						esc(vendorPhone),

						safeAmt(d.getAmount()), safeAmt(taxTotal), safeAmt(d.getNetPayable()),

						taxRows);
	}

	// -------- Helpers --------

	private static String esc(String s) {
		return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}

	private static String safeAmt(Double d) {
		return String.format("%,.2f", d == null ? 0.0 : d);
	}

	private static String safePct(Double d) {
		double v = d == null ? 0.0 : d;
		return String.format("%,.2f", v) + "%";
	}

	private static String fmtDate(LocalDate date) {
		return (date == null) ? "" : date.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"));
	}

	private static String fmtDateTime(LocalDateTime dt) {
		return (dt == null) ? "" : dt.format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));
	}
}