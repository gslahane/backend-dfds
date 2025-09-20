package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_fund_transfer")
public class FundTransfer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ✅ Map to Fund Demand
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fund_demand_id", referencedColumnName = "id", nullable = false, unique = true)
	private FundDemand fundDemand;

	@Column(nullable = false)
	private String demandId; // Copy from FundDemand

	@Column(nullable = false)
	private Double transferAmount; // Net Payable or Total Transfer Amount

	@Column(nullable = false)
	private String debitAccountNumber; // State Debit Account Number

	@Column(nullable = false)
	private String modeOfPayment; // e.g., I = IMPS / RTGS / NEFT

	@Column(nullable = false)
	private String beneficiaryAccountNumber;

	@Column(nullable = false)
	private String beneficiaryName;

	private String beneficiaryAddress1;
	private String beneficiaryAddress2;
	private String beneficiaryAddress3;
	private String beneficiaryPinCode;
	private String beneficiaryMobileNo;
	private String beneficiaryEmail;

	@Column(nullable = true)
	private String ddPayableAt;

	private String beneficiaryIfsc;
	private String branchName;
	private String bankName;
	private String beneficiaryAccountType; // SB / CA

	private String narration1; // e.g., "CA-01 Compensatory Afforestation"
	private String narration2; // e.g., "First Year Operations"

	@Column(nullable = false, unique = true)
	private String paymentRefNo; // Unique payment reference

	@CreationTimestamp
	private LocalDateTime createdAt;
}
