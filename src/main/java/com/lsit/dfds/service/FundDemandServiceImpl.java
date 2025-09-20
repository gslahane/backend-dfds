package com.lsit.dfds.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.FundDemandDetailBundleDto;
import com.lsit.dfds.dto.FundDemandListItemDto;
import com.lsit.dfds.dto.FundDemandQueryDto;
import com.lsit.dfds.dto.FundDemandRequestDto;
import com.lsit.dfds.dto.FundDemandResponseDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateResult;
import com.lsit.dfds.dto.FundTransferExecuteDto;
import com.lsit.dfds.dto.TaxDetailDto;
import com.lsit.dfds.dto.TaxTransferExecuteDto;
import com.lsit.dfds.dto.VendorDetailDto;
import com.lsit.dfds.dto.WorkDetailDto;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.FundTransferVoucher;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.TaxDeductionFundTransfer;
import com.lsit.dfds.entity.TaxDetail;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.FundDemandRepository;
import com.lsit.dfds.repo.FundTransferRepository;
import com.lsit.dfds.repo.FundTransferVoucherRepository;
import com.lsit.dfds.repo.TaxDeductionFundTransferRepository;
import com.lsit.dfds.repo.UserBankAccountRepository;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;
import com.lsit.dfds.repo.WorkRepository;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FundDemandServiceImpl implements FundDemandService {

	private static final String TAX_STATUS_GENERATED = "Generated";
	private static final String TAX_STATUS_READY_TO_TRANSFER = "ReadyToTransfer";
	private static final String TAX_STATUS_TRANSFERRED = "Transferred";
	private static final String TAX_STATUS_CANCELLED = "Cancelled";

	private final FundDemandRepository fundDemandRepository;
	private final WorkRepository workRepository;
	private final VendorRepository vendorRepository;
	private final TaxDeductionFundTransferRepository taxRepo;
	private final FundTransferRepository fundTransferRepository;
	private final FinancialYearRepository financialYearRepository;
	private final UserRepository userRepository;
	private final UserBankAccountRepository userBankAccountRepository;
	private final JwtUtil jwtUtil;

	// vouchers
	private final FundTransferVoucherRepository voucherRepo;
	private final VoucherTemplateService voucherTemplate;

	// ---------- Helpers ----------
	private User getUser(String username) {
		return userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	private boolean isStateRole(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private boolean isDistrictRole(User u) {
		if (u.getRole() == null)
			return false;
		return switch (u.getRole()) {
		case DISTRICT_ADMIN, DISTRICT_CHECKER, DISTRICT_MAKER, DISTRICT_ADPO, DISTRICT_DPO, DISTRICT_COLLECTOR -> true;
		default -> false;
		};
	}

	private void validateIAAdmin(HttpServletRequest request) {
		String username = jwtUtil.extractUsernameFromRequest(request);
		User user = getUser(username);
		if (user.getRole() != Roles.IA_ADMIN) {
			throw new RuntimeException("Unauthorized – Only IA Admin can perform this action");
		}
	}

	private java.util.Optional<UserBankAccount> primaryAccount(User u) {
		return userBankAccountRepository.findFirstByUser_IdAndIsPrimaryTrueAndStatus(u.getId(), Statuses.ACTIVE);
	}

	private static double nz(Double v) {
		return v == null ? 0.0 : v;
	}

	private static double sumTax(List<TaxDetail> taxes) {
		if (taxes == null)
			return 0.0;
		return taxes.stream().mapToDouble(t -> nz(t.getTaxAmount())).sum();
	}

	private List<TaxDetail> mapTaxes(List<TaxDetailDto> dtos) {
		return (dtos == null) ? List.of() : dtos.stream().map(t -> {
			TaxDetail td = new TaxDetail();
			td.setTaxName(t.getTaxName());
			td.setTaxPercentage(nz(t.getTaxPercentage()));
			td.setTaxAmount(nz(t.getTaxAmount()));
			return td;
		}).toList();
	}

	private void assertVendorConsistencyWithWork(Work work, Vendor vendor) {
		if (work.getVendor() != null && !Objects.equals(work.getVendor().getId(), vendor.getId())) {
			throw new RuntimeException("Demand vendor must match the vendor assigned to the work");
		}
	}

	private void assertWorkDemandGates(Work work) {
		if (work.getSanctionDate() == null) {
			throw new RuntimeException("Fund demand allowed only after Admin Approval");
		}
		if (work.getStatus() == null) {
			throw new RuntimeException("Work status is not set");
		}
		switch (work.getStatus()) {
		case AA_APPROVED, WORK_ORDER_ISSUED, IN_PROGRESS -> {
			/* ok */ }
		default -> throw new RuntimeException("Fund demand allowed only after Admin Approval");
		}
		if (java.time.LocalDate.now().isAfter(work.getSanctionDate().plusYears(2))) {
			throw new RuntimeException("Fund demand window closed (beyond 2 years from AA sanction)");
		}
	}

	private void assertIaDistrictConsistency(Work work, User iaUser) {
		if (work.getDistrict() == null || iaUser.getDistrict() == null
				|| !Objects.equals(work.getDistrict().getId(), iaUser.getDistrict().getId())) {
			throw new RuntimeException("IA must belong to the same district as the work");
		}
		if (work.getImplementingAgency() != null
				&& !Objects.equals(work.getImplementingAgency().getId(), iaUser.getId())) {
			throw new RuntimeException("This work is not assigned to the logged-in IA");
		}
	}

	private FinancialYear resolveWorkFinancialYear(Work work) {
		return java.util.Optional.ofNullable(work.getScheme()).map(Schemes::getFinancialYear)
				.orElseThrow(() -> new RuntimeException("Financial Year not linked to the Work's Scheme"));
	}

	private void syncTaxRecord(FundDemand demand, List<TaxDetail> taxes, String username) {
		double taxTotal = sumTax(taxes);

		var existing = taxRepo.findByFundDemand_Id(demand.getId());
		TaxDeductionFundTransfer t = existing.orElseGet(TaxDeductionFundTransfer::new);
		if (t.getId() == null) {
			t.setFundDemand(demand);
		}

		var work = demand.getWork();
		var vendor = demand.getVendor();

		t.setSchemeName(work != null && work.getScheme() != null ? work.getScheme().getSchemeName() : null);
		t.setWorkName(work != null ? work.getWorkName() : null);
		t.setWorkCode(work != null ? work.getWorkCode() : null);
		t.setVendorName(vendor != null ? vendor.getName() : null);

		// IA primary account (for tax transfer)
		String iaAccNo = null, iaIfsc = null;
		if (work != null && work.getImplementingAgency() != null) {
			var acc = primaryAccount(work.getImplementingAgency()).orElse(null);
			if (acc != null) {
				iaAccNo = acc.getAccountNumber();
				iaIfsc = acc.getIfsc();
			}
		}
		t.setBeneficiaryAccountNumber(iaAccNo);
		t.setBeneficiaryIfsc(iaIfsc);

		// Do not override debit account here; it is set during ReadyToTransfer/execute
		// time.
		t.setTotalTaxAmount(taxTotal);
		t.setTaxes(taxes);

		// If already transferred, don't regress status; otherwise keep at Generated
		if (!TAX_STATUS_TRANSFERRED.equals(t.getStatus())) {
			t.setStatus(TAX_STATUS_GENERATED);
		}
		if (t.getCreatedBy() == null) {
			t.setCreatedBy(username);
		}
		taxRepo.save(t);
	}

	// ------------------------------------------------ Create
	// ----------------------------------------------
	@Override
	@Transactional
	public FundDemand createFundDemand(FundDemandRequestDto dto, HttpServletRequest request) {
		validateIAAdmin(request);
		String username = jwtUtil.extractUsernameFromRequest(request);
		User iaUser = getUser(username);

		Work work = workRepository.findById(dto.getWorkId()).orElseThrow(() -> new RuntimeException("Work not found"));
		Vendor vendor = vendorRepository.findById(dto.getVendorId())
				.orElseThrow(() -> new RuntimeException("Vendor not found"));

		assertWorkDemandGates(work);
		assertIaDistrictConsistency(work, iaUser);
		assertVendorConsistencyWithWork(work, vendor);

		// FY
		FinancialYear financialYear = resolveWorkFinancialYear(work);

		// Budget reserve
		double amount = nz(dto.getAmount());
		if (amount <= 0)
			throw new RuntimeException("Demand amount must be greater than 0");
		double currentBalance = nz(work.getBalanceAmount());
		if (amount > currentBalance)
			throw new RuntimeException("Demand exceeds available balance");
		work.setBalanceAmount(currentBalance - amount);
		workRepository.save(work);

		// Taxes & net payable
		List<TaxDetail> taxDetails = mapTaxes(dto.getTaxes());
		double taxTotal = sumTax(taxDetails);
		double netPayable = dto.getNetPayable() != null ? dto.getNetPayable() : (amount - taxTotal);
		if (netPayable < 0)
			throw new RuntimeException("Net payable cannot be negative");

		// Persist demand
		FundDemand demand = new FundDemand();
		demand.setDemandId("DM-" + System.currentTimeMillis());
		demand.setWork(work);
		demand.setVendor(vendor);
		demand.setAmount(amount);
		demand.setNetPayable(netPayable);
		demand.setTaxes(taxDetails);
		demand.setDemandDate(dto.getDemandDate() != null ? dto.getDemandDate() : java.time.LocalDate.now());
		demand.setRemarks(dto.getRemarks());
		demand.setStatus(DemandStatuses.PENDING);
		demand.setCreatedBy(username);
		demand.setFinancialYear(financialYear);

		FundDemand saved = fundDemandRepository.save(demand);

		// Tax record
		syncTaxRecord(saved, taxDetails, username);
		return saved;
	}

	// ---------- Update (IA only while not approved by State) ----------
	@Transactional
	public FundDemand updateFundDemand(Long id, FundDemandRequestDto dto, HttpServletRequest request) {
		validateIAAdmin(request);
		String username = jwtUtil.extractUsernameFromRequest(request);
		User iaUser = getUser(username);

		FundDemand demand = fundDemandRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Fund demand not found"));

		// Allow IA edit only in early/returned states (not after State approval)
		DemandStatuses cs = demand.getStatus();
		boolean editable = switch (cs) {
		case PENDING, SENT_BACK_BY_DISTRICT, SENT_BACK_BY_STATE, REJECTED_BY_DISTRICT, REJECTED_BY_STATE -> true;
		default -> false;
		};
		if (!editable) {
			throw new RuntimeException("Demand cannot be edited at current status: " + cs);
		}

		Work work = demand.getWork();
		Vendor newVendor = vendorRepository.findById(dto.getVendorId())
				.orElseThrow(() -> new RuntimeException("Vendor not found"));
		assertWorkDemandGates(work);
		assertIaDistrictConsistency(work, iaUser);
		assertVendorConsistencyWithWork(work, newVendor);

		// Budget delta
		double oldAmount = nz(demand.getAmount());
		double newAmount = nz(dto.getAmount());
		if (newAmount <= 0)
			throw new RuntimeException("Demand amount must be greater than 0");
		double delta = newAmount - oldAmount;
		if (delta > 0) {
			// need additional reserve
			double bal = nz(work.getBalanceAmount());
			if (delta > bal)
				throw new RuntimeException("Insufficient work balance for update");
			work.setBalanceAmount(bal - delta);
		} else if (delta < 0) {
			// release back to work
			work.setBalanceAmount(nz(work.getBalanceAmount()) + (-delta));
		}
		workRepository.save(work);

		// Taxes & net payable
		List<TaxDetail> taxDetails = mapTaxes(dto.getTaxes());
		double taxTotal = sumTax(taxDetails);
		double netPayable = dto.getNetPayable() != null ? dto.getNetPayable() : (newAmount - taxTotal);
		if (netPayable < 0)
			throw new RuntimeException("Net payable cannot be negative");

		// Apply to demand
		demand.setVendor(newVendor);
		demand.setAmount(newAmount);
		demand.setNetPayable(netPayable);
		demand.setTaxes(taxDetails);
		demand.setDemandDate(dto.getDemandDate() != null ? dto.getDemandDate() : demand.getDemandDate());
		demand.setRemarks(dto.getRemarks());

		// If previously rejected, keep status as is (caller will resubmit via workflow)
		// If previously sent back, still editable; status unchanged here.
		FundDemand saved = fundDemandRepository.save(demand);

		// tax record (regenerate/refresh unless already transferred)
		syncTaxRecord(saved, taxDetails, username);
		return saved;
	}

	// ---------- List (entities) ----------
	@Override
	public List<FundDemand> getFundDemandsForUser(HttpServletRequest request) {
		String username = jwtUtil.extractUsernameFromRequest(request);
		User user = getUser(username);

		Roles role = user.getRole();
		if (isStateRole(user)) {
			return fundDemandRepository.findByIsDeleteFalse();
		} else if (isDistrictRole(user)) {
			Long districtId = user.getDistrict() != null ? user.getDistrict().getId() : null;
			return fundDemandRepository.findByIsDeleteFalse().stream()
					.filter(fd -> fd.getWork() != null && fd.getWork().getDistrict() != null
							&& Objects.equals(fd.getWork().getDistrict().getId(), districtId))
					.toList();
		} else if (role == Roles.IA_ADMIN) {
			return fundDemandRepository.findByCreatedByAndIsDeleteFalse(username);
		} else if (role == Roles.VENDOR) {
			return fundDemandRepository.findByIsDeleteFalse().stream()
					.filter(fd -> fd.getVendor() != null && fd.getVendor().getEmail() != null && user.getEmail() != null
							&& user.getEmail().equalsIgnoreCase(fd.getVendor().getEmail()))
					.toList();
		}
		throw new RuntimeException("Role not authorized to view demands");
	}

	@Override
	public List<FundDemandListItemDto> getFundDemandsForUserAsList(HttpServletRequest request) {
		return getFundDemandsForUser(request).stream().sorted(Comparator.comparing(FundDemand::getCreatedAt).reversed())
				.map(this::toListItem).toList();
	}

	// ---------- Delete ----------
	@Override
	@Transactional
	public String deleteFundDemand(Long id, HttpServletRequest request) {
		validateIAAdmin(request);

		FundDemand demand = fundDemandRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Fund demand not found"));

		if (Boolean.TRUE.equals(demand.getIsDelete())) {
			return "Demand already deleted";
		}

		// Deletable states only
		if (!(demand.getStatus() == DemandStatuses.PENDING || demand.getStatus() == DemandStatuses.REJECTED_BY_STATE
				|| demand.getStatus() == DemandStatuses.REJECTED_BY_DISTRICT
				|| demand.getStatus() == DemandStatuses.SENT_BACK_BY_DISTRICT
				|| demand.getStatus() == DemandStatuses.SENT_BACK_BY_STATE)) {
			throw new RuntimeException("Demand cannot be deleted at current status");
		}

		demand.setIsDelete(true);
		fundDemandRepository.save(demand);

		Work work = demand.getWork();
		work.setBalanceAmount(nz(work.getBalanceAmount()) + nz(demand.getAmount()));
		workRepository.save(work);

		taxRepo.findByFundDemand_Id(id).ifPresent(t -> taxRepo.delete(t));
		return "Fund demand deleted and budget restored successfully.";
	}

	// ---------- Update Status (District / State decisions) ----------
	@Override
	@Transactional(noRollbackFor = RuntimeException.class)
	public FundDemandStatusUpdateResult updateFundDemandStatus(FundDemandStatusUpdateDto dto, String username) {
		User user = getUser(username);

		DemandStatuses newStatus = dto.getNewStatus();
		if (newStatus == null)
			throw new RuntimeException("newStatus is required");

		boolean districtRole = isDistrictRole(user);
		boolean stateRole = isStateRole(user);
		if (!districtRole && !stateRole) {
			throw new RuntimeException("Unsupported role for updating fund demand status");
		}

		// Validate target status per tier
		if (districtRole) {
			if (!(newStatus == DemandStatuses.APPROVED_BY_DISTRICT || newStatus == DemandStatuses.REJECTED_BY_DISTRICT
					|| newStatus == DemandStatuses.SENT_BACK_BY_DISTRICT)) {
				throw new RuntimeException("Invalid target status for District");
			}
		} else {
			if (!(newStatus == DemandStatuses.APPROVED_BY_STATE || newStatus == DemandStatuses.REJECTED_BY_STATE
					|| newStatus == DemandStatuses.SENT_BACK_BY_STATE)) {
				throw new RuntimeException("Invalid target status for State");
			}
		}

		// Eligible CURRENT statuses per tier
		final List<DemandStatuses> eligibleCurrentForDistrict = List.of(DemandStatuses.PENDING,
				DemandStatuses.SENT_BACK_BY_STATE, DemandStatuses.REJECTED_BY_STATE);
		final List<DemandStatuses> eligibleCurrentForState = List.of(DemandStatuses.APPROVED_BY_DISTRICT);

		// Resolve targets
		List<FundDemand> targets;
		if (Boolean.TRUE.equals(dto.getApproveAll())) {
			var elig = districtRole ? eligibleCurrentForDistrict : eligibleCurrentForState;
			var base = fundDemandRepository.findByIsDeleteFalseAndStatusIn(elig);

			if (districtRole) {
				Long dId = (user.getDistrict() != null) ? user.getDistrict().getId() : null;
				if (dId == null)
					throw new RuntimeException("District-scoped user has no district");
				targets = base.stream().filter(d -> d.getWork() != null && d.getWork().getDistrict() != null
						&& Objects.equals(d.getWork().getDistrict().getId(), dId)).toList();
			} else {
				targets = base;
			}

		} else if (dto.getFundDemandIds() != null && !dto.getFundDemandIds().isEmpty()) {
			targets = fundDemandRepository.findAllById(dto.getFundDemandIds());
		} else {
			Long id = dto.getFundDemandId();
			if (id == null)
				throw new RuntimeException("fundDemandId is required (or pass fundDemandIds/approveAll)");
			targets = List.of(fundDemandRepository.findById(id)
					.orElseThrow(() -> new RuntimeException("Fund Demand not found: " + id)));
		}

		FundDemandStatusUpdateResult result = new FundDemandStatusUpdateResult();
		if (targets.isEmpty()) {
			result.getErrors().add("No demands to process");
			return result;
		}

		for (FundDemand demand : targets) {
			try {
				// Scope check for district
				if (districtRole) {
					if (user.getDistrict() == null || demand.getWork() == null || demand.getWork().getDistrict() == null
							|| !Objects.equals(user.getDistrict().getId(), demand.getWork().getDistrict().getId())) {
						result.getErrors().add("Out of district scope: " + demand.getId());
						continue;
					}
				}

				// Current status eligibility
				DemandStatuses curr = demand.getStatus();
				boolean eligibleNow = districtRole ? eligibleCurrentForDistrict.contains(curr)
						: eligibleCurrentForState.contains(curr);

				if (!eligibleNow) {
					result.getErrors().add("Not eligible for this tier: id=" + demand.getId() + " current=" + curr);
					continue;
				}

				// Apply status + remarks
				demand.setStatus(newStatus);
				demand.setRemarks(dto.getRemarks());

				// On reject – restore budget and cancel tax
				if (newStatus == DemandStatuses.REJECTED_BY_DISTRICT || newStatus == DemandStatuses.REJECTED_BY_STATE) {
					Work work = demand.getWork();
					if (work != null) {
						work.setBalanceAmount(nz(work.getBalanceAmount()) + nz(demand.getAmount()));
						workRepository.save(work);
					}
					taxRepo.findByFundDemand_Id(demand.getId()).ifPresent(tax -> {
						tax.setStatus(TAX_STATUS_CANCELLED);
						taxRepo.save(tax);
					});
				}

				fundDemandRepository.save(demand);
				result.setUpdated(result.getUpdated() + 1);
				result.getUpdatedIds().add(demand.getId());

			} catch (Exception ex) {
				result.getErrors().add("Failed id=" + demand.getId() + ": " + ex.getMessage());
			}
		}

		return result;
	}

	// ---------- Search / Detail / Per-work ----------
	@Override
	public List<FundDemandListItemDto> search(FundDemandQueryDto q, String username) {
		User user = getUser(username);
		boolean state = isStateRole(user);
		boolean district = isDistrictRole(user);

		List<FundDemand> base = fundDemandRepository.findByIsDeleteFalse();

		if (district) {
			Long districtId = (user.getDistrict() != null) ? user.getDistrict().getId() : null;
			base = base.stream().filter(d -> d.getWork() != null && d.getWork().getDistrict() != null
					&& Objects.equals(d.getWork().getDistrict().getId(), districtId)).toList();
		} else if (user.getRole() == Roles.IA_ADMIN) {
			base = fundDemandRepository.findByCreatedByAndIsDeleteFalse(user.getUsername());
		} else if (user.getRole() == Roles.VENDOR) {
			base = base
					.stream().filter(fd -> fd.getVendor() != null && fd.getVendor().getEmail() != null
							&& user.getEmail() != null && user.getEmail().equalsIgnoreCase(fd.getVendor().getEmail()))
					.toList();
		}

		if (q.getFinancialYearId() != null) {
			base = base.stream().filter(d -> d.getFinancialYear() != null
					&& Objects.equals(d.getFinancialYear().getId(), q.getFinancialYearId())).toList();
		}
		if (state && q.getDistrictId() != null) {
			base = base.stream().filter(d -> d.getWork() != null && d.getWork().getDistrict() != null
					&& Objects.equals(d.getWork().getDistrict().getId(), q.getDistrictId())).toList();
		}
		if (q.getStatus() != null) {
			base = base.stream().filter(d -> d.getStatus() == q.getStatus()).toList();
		}
		if (q.getQ() != null && !q.getQ().isBlank()) {
			String s = q.getQ().toLowerCase(Locale.ROOT);
			base = base.stream().filter(d -> {
				String did = d.getDemandId() != null ? d.getDemandId().toLowerCase(Locale.ROOT) : "";
				String workName = (d.getWork() != null && d.getWork().getWorkName() != null)
						? d.getWork().getWorkName().toLowerCase(Locale.ROOT)
						: "";
				String workCode = (d.getWork() != null && d.getWork().getWorkCode() != null)
						? d.getWork().getWorkCode().toLowerCase(Locale.ROOT)
						: "";
				String vendorName = (d.getVendor() != null && d.getVendor().getName() != null)
						? d.getVendor().getName().toLowerCase(Locale.ROOT)
						: "";
				return did.contains(s) || workName.contains(s) || workCode.contains(s) || vendorName.contains(s);
			}).toList();
		}

		return base.stream().sorted(Comparator.comparing(FundDemand::getCreatedAt).reversed()).map(this::toListItem)
				.toList();
	}

	@Override
	public FundDemandResponseDto getDemand(Long id, String username) {
		User user = getUser(username);
		FundDemand d = fundDemandRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Fund demand not found"));

		// Scope check
		if (isDistrictRole(user)) {
			if (user.getDistrict() == null || d.getWork() == null || d.getWork().getDistrict() == null
					|| !Objects.equals(user.getDistrict().getId(), d.getWork().getDistrict().getId())) {
				throw new RuntimeException("Forbidden");
			}
		} else if (user.getRole() == Roles.IA_ADMIN) {
			if (!Objects.equals(user.getUsername(), d.getCreatedBy())) {
				throw new RuntimeException("Forbidden");
			}
		} else if (user.getRole() == Roles.VENDOR) {
			if (d.getVendor() == null || d.getVendor().getEmail() == null || user.getEmail() == null
					|| !user.getEmail().equalsIgnoreCase(d.getVendor().getEmail())) {
				throw new RuntimeException("Forbidden");
			}
		} // State = all

		return toResponseDto(d);
	}

	@Override
	public List<FundDemandListItemDto> listByWork(Long workId, String username) {
		User u = getUser(username);
		List<FundDemand> list = fundDemandRepository.findByWork_Id(workId);

		if (isDistrictRole(u)) {
			Long dId = u.getDistrict() != null ? u.getDistrict().getId() : null;
			list = list.stream().filter(fd -> fd.getWork() != null && fd.getWork().getDistrict() != null
					&& Objects.equals(fd.getWork().getDistrict().getId(), dId)).toList();
		} else if (u.getRole() == Roles.IA_ADMIN) {
			list = list.stream().filter(fd -> Objects.equals(u.getUsername(), fd.getCreatedBy())).toList();
		} else if (u.getRole() == Roles.VENDOR) {
			list = list
					.stream().filter(fd -> fd.getVendor() != null && fd.getVendor().getEmail() != null
							&& u.getEmail() != null && u.getEmail().equalsIgnoreCase(fd.getVendor().getEmail()))
					.toList();
		}
		return list.stream().sorted(Comparator.comparing(FundDemand::getCreatedAt).reversed()).map(this::toListItem)
				.toList();
	}

	// ---------- Vendor transfer (State) ----------
	@Transactional
	public FundTransfer executeVendorTransfer(FundTransferExecuteDto dto, String username) {
		User user = getUser(username);
		if (!isStateRole(user)) {
			throw new RuntimeException("Only State can execute vendor transfer");
		}

		FundDemand demand = fundDemandRepository.findById(dto.getFundDemandId())
				.orElseThrow(() -> new RuntimeException("Fund Demand not found"));

		if (demand.getStatus() != DemandStatuses.APPROVED_BY_STATE) {
			throw new RuntimeException("Vendor payment allowed only after APPROVED_BY_STATE");
		}

		var existing = fundTransferRepository.findByFundDemand_Id(demand.getId());
		if (existing.isPresent()) {
			return existing.get();
		}

		Vendor v = demand.getVendor();
		if (v == null) {
			throw new RuntimeException("Demand has no vendor mapped");
		}

		FundTransfer ft = new FundTransfer();
		ft.setFundDemand(demand);
		ft.setDemandId(demand.getDemandId());
		ft.setTransferAmount(demand.getNetPayable());
		ft.setDebitAccountNumber(dto.getDebitAccountNumber());
		ft.setModeOfPayment(dto.getModeOfPayment() != null ? dto.getModeOfPayment() : "NEFT");
		ft.setBeneficiaryAccountNumber(v.getBankAccountNumber());
		ft.setBeneficiaryName(v.getName());
		ft.setBeneficiaryIfsc(v.getBankIfsc());
		ft.setBankName(v.getBankName());
		ft.setBeneficiaryAccountType("CA");
		ft.setNarration1(dto.getNarration1());
		ft.setNarration2(dto.getNarration2());
		ft.setPaymentRefNo("PAY-" + System.currentTimeMillis());

		FundTransfer saved = fundTransferRepository.save(ft);

		demand.setStatus(DemandStatuses.TRANSFERRED);
		fundDemandRepository.save(demand);

		taxRepo.findByFundDemand_Id(demand.getId()).ifPresent(tax -> {
			tax.setStatus(TAX_STATUS_READY_TO_TRANSFER);
			if (dto.getDebitAccountNumber() != null) {
				tax.setDebitAccountNumber(dto.getDebitAccountNumber());
			}
			taxRepo.save(tax);
		});

		ensureVoucher(demand, saved);
		return saved;
	}

	// ---------- Tax transfer to IA (State) ----------
	@Transactional
	public TaxDeductionFundTransfer executeTaxTransfer(TaxTransferExecuteDto dto, String username) {
		User user = getUser(username);
		if (!isStateRole(user)) {
			throw new RuntimeException("Only State can execute tax transfer");
		}

		FundDemand demand = fundDemandRepository.findById(dto.getFundDemandId())
				.orElseThrow(() -> new RuntimeException("Fund Demand not found"));

		if (demand.getStatus() != DemandStatuses.TRANSFERRED) {
			throw new RuntimeException("Tax transfer allowed only after vendor payment transfer");
		}

		TaxDeductionFundTransfer tax = taxRepo.findByFundDemand_Id(demand.getId())
				.orElseThrow(() -> new RuntimeException("Tax deduction record not found"));

		if (!TAX_STATUS_READY_TO_TRANSFER.equals(tax.getStatus())) {
			throw new RuntimeException("Tax is not ready to transfer");
		}

		tax.setStatus(TAX_STATUS_TRANSFERRED);
		tax.setRemarks(dto.getRemarks());
		if (dto.getDebitAccountNumber() != null) {
			tax.setDebitAccountNumber(dto.getDebitAccountNumber());
		}
		return taxRepo.save(tax);
	}

	// ---------- Vouchers ----------
	private FundTransferVoucher ensureVoucher(FundDemand d, FundTransfer ft) {
		return voucherRepo.findByFundDemand_Id(d.getId()).orElseGet(() -> {
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
			// Fix: copy taxes to avoid shared references
			v.setTaxes(d.getTaxes() == null ? null : d.getTaxes().stream().map(t -> {
				TaxDetail copy = new TaxDetail();
				copy.setTaxName(t.getTaxName());
				copy.setTaxPercentage(t.getTaxPercentage());
				copy.setTaxAmount(t.getTaxAmount());
				return copy;
			}).toList());

			String html = voucherTemplate.renderHtml(d, ft, tax, v);
			v.setHtml(html != null ? html : "");

			v.setCreatedBy(d.getCreatedBy());
			return voucherRepo.save(v);
		});
	}

	// ---------- DTO mappers ----------
	@Override
	public FundDemandResponseDto toResponseDto(FundDemand d) {
		FundDemandResponseDto dto = new FundDemandResponseDto();
		dto.setDemandId(d.getDemandId());
		dto.setSchemeName(
				d.getWork() != null && d.getWork().getScheme() != null ? d.getWork().getScheme().getSchemeName()
						: null);
		dto.setSchemeType(d.getWork() != null && d.getWork().getScheme() != null
				&& d.getWork().getScheme().getSchemeType() != null ? d.getWork().getScheme().getBaseSchemeType().name()
						: null);
		dto.setFinancialYear(d.getFinancialYear() != null ? d.getFinancialYear().getFinacialYear() : null);
		dto.setWorkName(d.getWork() != null ? d.getWork().getWorkName() : null);
		dto.setWorkCode(d.getWork() != null ? d.getWork().getWorkCode() : null);
		dto.setVendorName(d.getVendor() != null ? d.getVendor().getName() : null);
		dto.setIaName(d.getCreatedBy());
		dto.setAmount(d.getAmount());
		dto.setNetPayable(d.getNetPayable());
		dto.setDemandStatus(d.getStatus() != null ? d.getStatus().name() : null);
		dto.setDemandDate(d.getDemandDate());
		dto.setMlaName(d.getWork() != null && d.getWork().getRecommendedByMla() != null
				? d.getWork().getRecommendedByMla().getMlaName()
				: null);
		dto.setMlcName(d.getWork() != null && d.getWork().getRecommendedByMlc() != null
				? d.getWork().getRecommendedByMlc().getMlcName()
				: null);
		if (d.getTaxes() != null) {
			dto.setTaxes(d.getTaxes().stream().map(t -> {
				TaxDetailDto x = new TaxDetailDto();
				x.setTaxName(t.getTaxName());
				x.setTaxPercentage(t.getTaxPercentage());
				x.setTaxAmount(t.getTaxAmount());
				return x;
			}).toList());
		}
		return dto;
	}

	// inside FundDemandServiceImpl
	@Override
	public FundDemandListItemDto toListItem(FundDemand d) {
		var w = d.getWork();

		String financialYear = (d.getFinancialYear() != null) ? d.getFinancialYear().getFinacialYear() : null;

		String planType = null;
		if (w != null && w.getScheme() != null && w.getScheme().getBaseSchemeType() != null) {
			planType = w.getScheme().getBaseSchemeType().name();
		}

		String districtName = (w != null && w.getDistrict() != null) ? w.getDistrict().getDistrictName() : null;
		String constituencyName = (w != null && w.getConstituency() != null) ? w.getConstituency().getConstituencyName()
				: null;

		Double workAaAmount = (w != null && w.getAdminApprovedAmount() != null) ? w.getAdminApprovedAmount() : 0.0;
		Double workGrossOrderAmount = (w != null && w.getGrossAmount() != null) ? w.getGrossAmount() : 0.0;

		String workName = (w != null) ? w.getWorkName() : null;
		String workCode = (w != null) ? w.getWorkCode() : null;

		String vendorName = (d.getVendor() != null) ? d.getVendor().getName() : null;

		// Demand amounts
		Double demandGrossAmount = (d.getAmount() != null) ? d.getAmount() : 0.0;

		double demandTaxTotal = 0.0;
		List<TaxDetailDto> demandTaxDetails = null;
		if (d.getTaxes() != null && !d.getTaxes().isEmpty()) {
			demandTaxTotal = d.getTaxes().stream().map(t -> t.getTaxAmount() == null ? 0.0 : t.getTaxAmount())
					.mapToDouble(Double::doubleValue).sum();

			demandTaxDetails = d.getTaxes().stream().map(t -> {
				TaxDetailDto x = new TaxDetailDto();
				x.setTaxName(t.getTaxName());
				x.setTaxPercentage(t.getTaxPercentage());
				x.setTaxAmount(t.getTaxAmount());
				return x;
			}).toList();
		}

		Double demandNetAmount = (d.getNetPayable() != null) ? d.getNetPayable() : (demandGrossAmount - demandTaxTotal);

		// MLA details
		Long mlaId = null;
		String mlaName = null;
		if (w != null && w.getRecommendedByMla() != null) {
			mlaId = w.getRecommendedByMla().getId();
			mlaName = w.getRecommendedByMla().getMlaName();
		}

		// MLC details
		Long mlcId = null;
		String mlcName = null;
		if (w != null && w.getRecommendedByMlc() != null) {
			mlcId = w.getRecommendedByMlc().getId();
			mlcName = w.getRecommendedByMlc().getMlcName();
		}

		// HADP details
		Long hadpId = null;
		String hadpName = null;
		if (w != null && w.getHadp() != null) {
			hadpId = w.getHadp().getId();
			hadpName = w.getHadp().getDescription(); // or use talukaName if more relevant
		}

		return FundDemandListItemDto.builder().fundDemandId(d.getId()).demandId(d.getDemandId())
				.financialYear(financialYear).planType(planType)

				.districtName(districtName).constituencyName(constituencyName)

				.workName(workName).workCode(workCode).workAaAmount(workAaAmount)
				.workGrossOrderAmount(workGrossOrderAmount)

				.vendorName(vendorName)

				.demandGrossAmount(demandGrossAmount).demandTaxTotal(demandTaxTotal).demandTaxDetails(demandTaxDetails)
				.demandNetAmount(demandNetAmount)

				.status(d.getStatus() != null ? d.getStatus().name() : null).demandDate(d.getDemandDate())
				.createdAt(d.getCreatedAt()).createdBy(d.getCreatedBy())

				.mlaId(mlaId).mlaName(mlaName).mlcId(mlcId).mlcName(mlcName).hadpId(hadpId).hadpName(hadpName).build();
	}

	private WorkDetailDto toWorkDetailDto(Work w) {
		if (w == null)
			return null;

		String schemeName = (w.getScheme() != null) ? w.getScheme().getSchemeName() : null;
		String planType = (w.getScheme() != null && w.getScheme().getBaseSchemeType() != null)
				? w.getScheme().getBaseSchemeType().name()
				: null;
		String finYear = (w.getScheme() != null && w.getScheme().getFinancialYear() != null)
				? w.getScheme().getFinancialYear().getFinacialYear()
				: null;

		String districtName = (w.getDistrict() != null) ? w.getDistrict().getDistrictName() : null;
		String constituency = (w.getConstituency() != null) ? w.getConstituency().getConstituencyName() : null;

		String status = (w.getStatus() != null) ? w.getStatus().name() : null;

		Long iaId = (w.getImplementingAgency() != null) ? w.getImplementingAgency().getId() : null;
		String iaName = (w.getImplementingAgency() != null) ? w.getImplementingAgency().getFullname() : null;

		return WorkDetailDto.builder().workId(w.getId()).workName(w.getWorkName()).workCode(w.getWorkCode())
				.schemeName(schemeName).planType(planType).financialYear(finYear).districtName(districtName)
				.constituencyName(constituency).aaAmount(w.getAdminApprovedAmount())
				.grossWorkOrderAmount(w.getGrossAmount()).balanceAmount(w.getBalanceAmount())
				.sanctionDate(w.getSanctionDate()).workStatus(status).iaUserId(iaId).iaUserName(iaName).build();
	}

	private VendorDetailDto toVendorDetailDto(Vendor v) {
		if (v == null)
			return null;
		return VendorDetailDto.builder().vendorId(v.getId()).name(v.getName()).contactPerson(v.getContactPerson())
				.phone(v.getPhone()).email(v.getEmail()).gstNumber(v.getGstNumber())
				.gstVerified(Boolean.TRUE.equals(v.getGstVerified())).panNumber(v.getPanNumber())
				.address(v.getAddress()).city(v.getCity()).state(v.getState()).pincode(v.getPincode())
				.bankAccountNumber(v.getBankAccountNumber()).bankIfsc(v.getBankIfsc()).bankName(v.getBankName())
				.bankBranch(v.getBankBranch()).paymentEligible(Boolean.TRUE.equals(v.getPaymentEligible())).build();
	}

	@Override
	public FundDemandDetailBundleDto getDemandBundle(Long id, String username) {
		// reuse the same scope rules as getDemand
		User user = getUser(username);
		FundDemand d = fundDemandRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Fund demand not found"));

		// Scope check
		if (isDistrictRole(user)) {
			if (user.getDistrict() == null || d.getWork() == null || d.getWork().getDistrict() == null
					|| !Objects.equals(user.getDistrict().getId(), d.getWork().getDistrict().getId())) {
				throw new RuntimeException("Forbidden");
			}
		} else if (user.getRole() == Roles.IA_ADMIN) {
			if (!Objects.equals(user.getUsername(), d.getCreatedBy())) {
				throw new RuntimeException("Forbidden");
			}
		} else if (user.getRole() == Roles.VENDOR) {
			if (d.getVendor() == null || d.getVendor().getEmail() == null || user.getEmail() == null
					|| !user.getEmail().equalsIgnoreCase(d.getVendor().getEmail())) {
				throw new RuntimeException("Forbidden");
			}
		}
		// STATE = all

		// Build lists (each is single-element except taxes which may be many)
		var demandDto = toResponseDto(d);
		var workDto = toWorkDetailDto(d.getWork());
		var vendorDto = toVendorDetailDto(d.getVendor());

		var taxes = (d.getTaxes() == null) ? List.<TaxDetailDto>of() : d.getTaxes().stream().map(t -> {
			TaxDetailDto x = new TaxDetailDto();
			x.setTaxName(t.getTaxName());
			x.setTaxPercentage(t.getTaxPercentage());
			x.setTaxAmount(t.getTaxAmount());
			return x;
		}).toList();

		return FundDemandDetailBundleDto.builder().demands(List.of(demandDto))
				.works(workDto != null ? List.of(workDto) : List.of())
				.vendors(vendorDto != null ? List.of(vendorDto) : List.of()).taxes(taxes).build();
	}

}
