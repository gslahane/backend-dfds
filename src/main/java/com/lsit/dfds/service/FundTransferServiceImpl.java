package com.lsit.dfds.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.ApproveAndGeneratePaymentDto;
import com.lsit.dfds.dto.FundTransferExecuteDto;
import com.lsit.dfds.dto.PaymentFileGenerateDto;
import com.lsit.dfds.dto.PaymentFileResult;
import com.lsit.dfds.dto.TaxTransferExecuteDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.entity.TaxDeductionFundTransfer;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.FundTransferRepository;
import com.lsit.dfds.repo.FundTransferVoucherRepository;
import com.lsit.dfds.repo.TaxDeductionFundTransferRepository;
import com.lsit.dfds.repo.UserBankAccountRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.utils.H2HSftpClient;
import com.lsit.dfds.utils.PaymentFileBuilder;
import com.lsit.dfds.utils.PaymentFileBuilder.PaymentRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundTransferServiceImpl implements FundTransferService {

	private final FundDemandRepository fundDemandRepository;
	private final FundTransferRepository fundTransferRepository;
	private final TaxDeductionFundTransferRepository taxRepo;
	private final UserRepository userRepository;
	private final UserBankAccountRepository userBankAccountRepository;
	private final FundTransferVoucherRepository voucherRepo;
	private final VoucherTemplateService voucherTemplate;

	private final PaymentFileBuilder paymentFileBuilder;
	private final StateAccountResolver stateAccountResolver;

	private final @Qualifier("bankSftpClient") H2HSftpClient h2hSftpClient;

	private User getUser(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	private boolean isStateRole(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private Optional<UserBankAccount> primaryAccount(User u) {
		return userBankAccountRepository.findFirstByUser_IdAndIsPrimaryTrueAndStatus(u.getId(), Statuses.ACTIVE);
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	private static String toModeCode(String s) {
		if (s == null)
			return "N";
		s = s.trim().toUpperCase(Locale.ROOT);
		return switch (s) {
		case "RTGS", "R" -> "R";
		case "IMPS", "I" -> "I";
		default -> "N"; // NEFT
		};
	}

	private static String modeFromCode(String c) {
		return "R".equals(c) ? "RTGS" : "I".equals(c) ? "IMPS" : "NEFT";
	}

	private String narrWithWorkCode(String base, FundDemand d) {
		String wc = (d.getWork() != null && d.getWork().getWorkCode() != null) ? d.getWork().getWorkCode() : "";
		return (wc == null || wc.isBlank()) ? base : base + " - " + wc;
	}

	private String ensureVoucher(FundDemand d, FundTransfer ft) {
		return voucherRepo.findByFundDemand_Id(d.getId()).map(v -> "").orElseGet(() -> {
			var tax = taxRepo.findByFundDemand_Id(d.getId()).orElse(null);

			FundTransferVoucher v = new FundTransferVoucher();
			v.setFundDemand(d);
			v.setVoucherNo("VCH-" + System.currentTimeMillis());
			v.setDemandId(d.getDemandId());

			if (d.getWork() != null) {
				v.setWorkName(d.getWork().getWorkName());
				v.setWorkCode(d.getWork().getWorkCode());
				if (d.getWork().getScheme() != null) {
					v.setSchemeName(d.getWork().getScheme().getSchemeName());
				}
			}
			if (d.getVendor() != null) {
				v.setVendorName(d.getVendor().getName());
				v.setVendorAccountNumber(d.getVendor().getBankAccountNumber());
				v.setVendorIfsc(d.getVendor().getBankIfsc());
				v.setVendorBankName(d.getVendor().getBankName());
			}

			double taxTotal = (d.getTaxes() == null) ? 0.0
					: d.getTaxes().stream().mapToDouble(t -> t.getTaxAmount() == null ? 0.0 : t.getTaxAmount()).sum();

			v.setGrossAmount(d.getAmount());
			v.setTaxTotal(taxTotal);
			v.setNetPayable(d.getNetPayable());
			v.setTaxes(d.getTaxes() == null ? null : new ArrayList<>(d.getTaxes()));

			String html = voucherTemplate.renderHtml(d, ft, tax, v); // get html here
			v.setHtml(""); // Do not persist HTML
			v.setCreatedBy(d.getCreatedBy());
			voucherRepo.save(v);
			return html;
		});
	}

	// ---------- Optional single-demand (backend resolves debit) ----------
	@Override
	@Transactional
	public FundTransfer executeVendorTransfer(FundTransferExecuteDto dto, String username) {
		User user = getUser(username);
		if (!isStateRole(user))
			throw new RuntimeException("Only State can execute vendor transfer");

		FundDemand d = fundDemandRepository.findById(dto.getFundDemandId())
				.orElseThrow(() -> new RuntimeException("Fund Demand not found"));

		if (d.getStatus() != DemandStatuses.APPROVED_BY_STATE) {
			throw new RuntimeException("Vendor payment allowed only after APPROVED_BY_STATE");
		}

		String debitVendor = stateAccountResolver.vendorDebitAccount();

		var existing = fundTransferRepository.findByFundDemand_Id(d.getId());
		if (existing.isPresent())
			return existing.get();

		Vendor v = d.getVendor();
		if (v == null)
			throw new RuntimeException("Demand has no vendor mapped");

		String modeCode = toModeCode(dto.getModeOfPayment());
		FundTransfer ft = new FundTransfer();
		ft.setFundDemand(d);
		ft.setDemandId(d.getDemandId());
		ft.setTransferAmount(d.getNetPayable());
		ft.setDebitAccountNumber(debitVendor);
		ft.setModeOfPayment(modeFromCode(modeCode));
		ft.setBeneficiaryAccountNumber(v.getBankAccountNumber());
		ft.setBeneficiaryName(v.getName());
		ft.setBeneficiaryIfsc(v.getBankIfsc());
		ft.setBankName(v.getBankName());
		ft.setBeneficiaryAccountType("CA");
		ft.setNarration1(dto.getNarration1());
		ft.setNarration2(dto.getNarration2());
		ft.setPaymentRefNo("PAY-" + System.currentTimeMillis());

		FundTransfer saved = fundTransferRepository.save(ft);

		d.setStatus(DemandStatuses.TRANSFERRED);
		fundDemandRepository.save(d);

		taxRepo.findByFundDemand_Id(d.getId()).ifPresent(tax -> {
			tax.setDebitAccountNumber(stateAccountResolver.taxDebitAccount());
			tax.setStatus("Transferred"); // same file run does tax as well
			taxRepo.save(tax);
		});

		ensureVoucher(d, saved);
		return saved;
	}

	@Override
	@Transactional
	public TaxDeductionFundTransfer executeTaxTransfer(TaxTransferExecuteDto dto, String username) {
		User user = getUser(username);
		if (!isStateRole(user))
			throw new RuntimeException("Only State can execute tax transfer");

		FundDemand d = fundDemandRepository.findById(dto.getFundDemandId())
				.orElseThrow(() -> new RuntimeException("Fund Demand not found"));

		if (d.getStatus() != DemandStatuses.TRANSFERRED) {
			throw new RuntimeException("Tax transfer allowed only after vendor payment transfer");
		}

		TaxDeductionFundTransfer tax = taxRepo.findByFundDemand_Id(d.getId())
				.orElseThrow(() -> new RuntimeException("Tax deduction record not found"));

		// If not already set by batch
		if (!"Transferred".equalsIgnoreCase(tax.getStatus())) {
			tax.setDebitAccountNumber(stateAccountResolver.taxDebitAccount());
			tax.setStatus("Transferred");
			tax.setRemarks(dto.getRemarks());
		}
		return taxRepo.save(tax);
	}

	// ---------- Internal: build rows & persist transfers ----------
	private List<PaymentRow> buildAndPersistTransfers(List<FundDemand> demands, String modeCode, String narration1,
			String narration2, String debitVendor, String debitTax, List<Long> includedOut) {
		String modeStr = modeFromCode(modeCode);
		List<PaymentRow> rows = new ArrayList<>();

		for (FundDemand d : demands) {
			Vendor v = d.getVendor();
			if (v == null)
				throw new RuntimeException("Vendor missing for demand " + d.getId());
			if (v.getBankAccountNumber() == null || v.getBankIfsc() == null || v.getBankName() == null
					|| v.getBankBranch() == null) {
				throw new RuntimeException("Vendor bank details incomplete for demand " + d.getId());
			}

			// create vendor FundTransfer row (persist)
			FundTransfer ft = new FundTransfer();
			ft.setFundDemand(d);
			ft.setDemandId(d.getDemandId());
			ft.setTransferAmount(d.getNetPayable());
			ft.setDebitAccountNumber(debitVendor);
			ft.setModeOfPayment(modeStr);
			ft.setBeneficiaryAccountNumber(v.getBankAccountNumber());
			ft.setBeneficiaryName(v.getName());
			ft.setBeneficiaryIfsc(v.getBankIfsc());
			ft.setBankName(v.getBankName());
			ft.setBeneficiaryAccountType("CA");
			ft.setNarration1(narration1);
			ft.setNarration2(narration2);
			ft.setPaymentRefNo("PAY-" + System.currentTimeMillis() + "-" + d.getId());
			ft = fundTransferRepository.save(ft);

			// demand -> TRANSFERRED
			d.setStatus(DemandStatuses.TRANSFERRED);
			fundDemandRepository.save(d);

			// vendor row for file
			PaymentRow rV = new PaymentRow();
			rV.debitAccountNo = debitVendor;
			rV.modeCode = modeCode;
			rV.beneficiaryAccountNo = v.getBankAccountNumber();
			rV.ifsc = v.getBankIfsc();
			rV.beneficiaryName = v.getName();
			rV.bank = v.getBankName();
			rV.branch = v.getBankBranch();
			rV.accountType = "CA";
			rV.amount = d.getNetPayable();
			rV.narration1 = narrWithWorkCode(narration1, d);
			rV.narration2 = narration2;
			rV.add1 = v.getAddress();
			rV.add2 = v.getCity();
			rV.add3 = v.getState();
			rV.pincode = v.getPincode();
			rV.mobile = v.getPhone();
			rV.email = v.getEmail();
			rV.paymentRefNo = ft.getPaymentRefNo();
			rows.add(rV);

			ensureVoucher(d, ft);

			// tax row → IA primary account (if tax > 0)
			TaxDeductionFundTransfer tax = taxRepo.findByFundDemand_Id(d.getId()).orElse(null);
			double taxTotal = (tax == null) ? 0.0 : nz(tax.getTotalTaxAmount());
			if (taxTotal > 0.0) {
				User ia = (d.getWork() != null) ? d.getWork().getImplementingAgency() : null;
				if (ia == null)
					throw new RuntimeException("IA user not linked for demand " + d.getId());

				UserBankAccount iaAcc = primaryAccount(ia)
						.orElseThrow(() -> new RuntimeException("IA primary bank missing for demand " + d.getId()));

				if (tax != null) {
					tax.setDebitAccountNumber(debitTax);
					tax.setStatus("Transferred");
					tax.setRemarks("Auto by payment file");
					taxRepo.save(tax);
				}

				PaymentRow rT = new PaymentRow();
				rT.debitAccountNo = debitTax;
				rT.modeCode = modeCode;
				rT.beneficiaryAccountNo = iaAcc.getAccountNumber();
				rT.ifsc = iaAcc.getIfsc();
				rT.beneficiaryName = ia.getFullname();
				rT.bank = iaAcc.getBankName();
				rT.branch = iaAcc.getBranch();
				rT.accountType = iaAcc.getAccountType() == null ? "CA" : iaAcc.getAccountType();
				rT.amount = taxTotal;
				rT.narration1 = "TAX-" + narrWithWorkCode(narration1, d);
				rT.narration2 = narration2;
				rT.add1 = "IA Primary Account";
			 rT.add2 = ia.getDesignation();
				rT.add3 = "";
				rT.pincode = "";
				rT.mobile = ia.getMobile() == null ? "" : String.valueOf(ia.getMobile());
				rT.email = ia.getEmail();
				rT.paymentRefNo = "TAX-" + System.currentTimeMillis() + "-" + d.getId();
				rows.add(rT);
			}

			includedOut.add(d.getId());
		}

		return rows;
	}

	@Override
	@Transactional
	public PaymentFileResult generatePaymentFileForApprovedDemands(PaymentFileGenerateDto req, String username)
			throws IOException {
		User user = getUser(username);
		if (!isStateRole(user))
			throw new RuntimeException("Only State can generate payment file");

		if (req.getFundDemandIds() == null || req.getFundDemandIds().isEmpty()) {
			throw new RuntimeException("No fund demand ids provided");
		}
		String n1 = (req.getNarration1() == null) ? "" : req.getNarration1().trim();
		if (n1.isBlank())
			throw new RuntimeException("Narration1 is required");

		String debitVendor = stateAccountResolver.vendorDebitAccount();
		String debitTax = stateAccountResolver.taxDebitAccount();
		String modeCode = toModeCode(req.getModeOfPayment());

		List<FundDemand> demands = fundDemandRepository.findAllById(req.getFundDemandIds());
		List<FundDemand> eligible = demands.stream()
				.filter(d -> d != null && d.getStatus() == DemandStatuses.APPROVED_BY_STATE)
				.filter(d -> fundTransferRepository.findByFundDemand_Id(d.getId()).isEmpty())
				.sorted(Comparator.comparing(FundDemand::getId)).toList();
		if (eligible.isEmpty())
			throw new RuntimeException("No eligible demands in APPROVED_BY_STATE");

		List<Long> included = new ArrayList<>();
		List<PaymentRow> rows = buildAndPersistTransfers(eligible, modeCode, n1, req.getNarration2(), debitVendor,
				debitTax, included);

		PaymentFileResult written = paymentFileBuilder.writePaymentFile(rows, req.getFilePrefix());
		return new PaymentFileResult(written.getFileName(), written.getAbsolutePath(), written.getTotalRows(),
				written.getTotalAmount(), included);
	}

	// >>> STATE approves selected/eligible demands, then immediately generates file
	// & persists transfers
	@Override
	@Transactional
	public PaymentFileResult approveAndGenerateFile(ApproveAndGeneratePaymentDto req, String username)
			throws IOException {

		User user = getUser(username);
		if (!isStateRole(user))
			throw new RuntimeException("Only State can approve & generate");

		String n1 = (req.getNarration1() == null) ? "" : req.getNarration1().trim();
		if (n1.isBlank())
			throw new RuntimeException("Narration1 is required");

		// Find set to approve (APPROVED_BY_DISTRICT -> APPROVED_BY_STATE)
		List<FundDemand> pool = fundDemandRepository.findByIsDeleteFalse();
		List<FundDemand> toApprove;

		if (Boolean.TRUE.equals(req.getApproveAll())) {
			toApprove = pool.stream().filter(d -> d.getStatus() == DemandStatuses.APPROVED_BY_DISTRICT)
					.sorted(Comparator.comparing(FundDemand::getId)).toList();
		} else if (req.getFundDemandIds() != null && !req.getFundDemandIds().isEmpty()) {
			List<FundDemand> selected = fundDemandRepository.findAllById(req.getFundDemandIds());
			toApprove = selected.stream().filter(d -> d.getStatus() == DemandStatuses.APPROVED_BY_DISTRICT)
					.sorted(Comparator.comparing(FundDemand::getId)).toList();
		} else {
			throw new RuntimeException("Provide fundDemandIds or approveAll=true");
		}
		if (toApprove.isEmpty())
			throw new RuntimeException("No eligible demands in APPROVED_BY_DISTRICT");

		// Flip to APPROVED_BY_STATE
		for (FundDemand d : toApprove) {
			d.setStatus(DemandStatuses.APPROVED_BY_STATE);
			fundDemandRepository.save(d);
		}

		// Then vendor+tax transfers and file build
		String debitVendor = stateAccountResolver.vendorDebitAccount();
		String debitTax = stateAccountResolver.taxDebitAccount();
		String modeCode = toModeCode(req.getModeOfPayment());

		List<Long> included = new ArrayList<>();
		List<PaymentRow> rows = buildAndPersistTransfers(toApprove, modeCode, n1, req.getNarration2(), debitVendor,
				debitTax, included);

//		PaymentFileResult written = paymentFileBuilder.writePaymentFile(rows, req.getFilePrefix());
//		return new PaymentFileResult(written.getFileName(), written.getAbsolutePath(), written.getTotalRows(),
//				written.getTotalAmount(), included);

		PaymentFileResult written = paymentFileBuilder.writePaymentFile(rows, req.getFilePrefix());
		return new PaymentFileResult(written.getFileName(), written.getAbsolutePath(), written.getTotalRows(),
				written.getTotalAmount(), included);

	}
}
