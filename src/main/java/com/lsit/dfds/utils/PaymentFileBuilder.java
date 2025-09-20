package com.lsit.dfds.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.lsit.dfds.dto.PaymentFileResult;
import org.springframework.stereotype.Component;

@Component
public class PaymentFileBuilder {

	/** One payment record per line (19 fields) */
	public static class PaymentRow {
		public String debitAccountNo; // 1 Debit Account No
		public String modeCode; // 2 N(NEFT)/R(RTGS)/I(IMPS)
		public String beneficiaryAccountNo; // 3
		public String beneficiaryName; // 4
		public Double amount; // 5
		public String add1; // 6 Benf Add1
		public String add2; // 7 Benf Add2
		public String add3; // 8 Benf Add3
		public String pincode; // 9 Benf Pincode
		public String mobile; // 10 Mobile_no
		public String email; // 11 Benf email ID
		public String ddPayableAt; // 12 DD_payable at (use "NA")
		public String ifsc; // 13 Benf IFSC
		public String branch; // 14 Branch Name
		public String bank; // 15 Bank Name
		public String accountType; // 16 Benf Account Type: SB/CA
		public String narration1; // 17
		public String narration2; // 18
		public String paymentRefNo; // 19
	}

	private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	public PaymentFileResult writePaymentFile(List<PaymentRow> rows, String filePrefix) throws IOException {
		if (rows == null || rows.isEmpty())
			throw new IllegalArgumentException("No rows to write");

		String prefix = (filePrefix == null || filePrefix.isBlank()) ? "PaymentFile" : filePrefix.trim();
		String fileName = prefix + "_" + TS.format(LocalDateTime.now()) + ".txt";

		Path dir = Paths.get(System.getProperty("java.io.tmpdir")); // or configured outbound folder
		Path out = dir.resolve(fileName);

		List<String> lines = new ArrayList<>();
		lines.add(headerLine());

		BigDecimal total = BigDecimal.ZERO;
		for (PaymentRow r : rows) {
			lines.add(rowLine(r));
			total = total.add(bd(r.amount));
		}

		Files.write(out, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
		return new PaymentFileResult(fileName, out.toAbsolutePath().toString(), rows.size(), total.doubleValue(), null);
	}

	private String headerLine() {
		return String.join("|",
				List.of("Debit Account No", "Mode Of Payment", "Beneficiary Account No", "Beneficiary Name", "Amount",
						"Benf Add1", "Benf Add2", "Benf Add3", "Benf Pincode", "Mobile_no", "Benf email ID",
						"DD_payable at", "Benf IFSC", "Branch Name", "Bank Name", "Benf Account Type", "Narration1",
						"Narration2", "payment Ref No"));
	}

	private String rowLine(PaymentRow r) {
		return String.join("|",
				List.of(sv(r.debitAccountNo), mode(r.modeCode), sv(r.beneficiaryAccountNo), sv(r.beneficiaryName),
						amt(r.amount), sv(r.add1), sv(r.add2), sv(r.add3), sv(r.pincode), sv(r.mobile), sv(r.email),
						na(r.ddPayableAt), upper(r.ifsc), sv(r.branch), sv(r.bank), acct(r.accountType),
						sv(r.narration1), sv(r.narration2), sv(r.paymentRefNo)));
	}

	private static String sv(String s) {
		if (s == null)
			return "";
		String x = s.replace("\r", " ").replace("\n", " ");
		x = x.replace("|", "/"); // protect delimiter
		return x.trim();
	}

	private static String upper(String s) {
		return s == null ? "" : s.trim().toUpperCase();
	}

	private static String na(String s) {
		return (s == null || s.isBlank()) ? "NA" : s.trim();
	}

	private static String mode(String s) {
		if (s == null)
			return "N";
		String m = s.trim().toUpperCase(Locale.ROOT);
		return (m.startsWith("R") ? "R" : m.startsWith("I") ? "I" : "N");
		// R=RTGS, I=IMPS, N=NEFT
	}

	private static String acct(String s) {
		if (s == null)
			return "CA";
		String t = s.trim().toUpperCase(Locale.ROOT);
		return t.startsWith("S") ? "SB" : "CA";
	}

	private static BigDecimal bd(Double d) {
		return BigDecimal.valueOf(d == null ? 0.0 : d).setScale(2, RoundingMode.HALF_UP);
	}

	private static String amt(Double d) {
		return bd(d).toPlainString();
	}
}
