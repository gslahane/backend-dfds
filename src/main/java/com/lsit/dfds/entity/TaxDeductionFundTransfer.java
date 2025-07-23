package com.lsit.dfds.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
@Table(name = "lsit_dfds_tax_deduction_fund_transfer")
public class TaxDeductionFundTransfer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Linked Fund Demand for which this tax deduction transfer is created.
	 */
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fund_demand_id", referencedColumnName = "id", nullable = false, unique = true)
	private FundDemand fundDemand;

	@Column(nullable = true)
	private String schemeName;

	@Column(nullable = true)
	private String workName;

	@Column(nullable = true)
	private String workCode;

	@Column(nullable = true)
	private String vendorName;

	/**
	 * Account number of the IA where tax deductions are transferred.
	 */
	@Column(nullable = true)
	private String beneficiaryAccountNumber;

	/**
	 * State/Tax Deduction Pool account from which tax amounts are transferred.
	 */
	@Column(nullable = true)
	private String debitAccountNumber;

	/**
	 * IFSC of the IA beneficiary bank.
	 */
	@Column(nullable = true)
	private String beneficiaryIfsc;

	/**
	 * IFSC of the debit (State) bank.
	 */
	@Column(nullable = true)
	private String debitBankIfsc;

	/**
	 * Total tax amount being transferred.
	 */
	private Double totalTaxAmount = 0.0;

	/**
	 * All tax details (name, percentage, amount).
	 */
	@ElementCollection
	@CollectionTable(name = "lsit_dfds_tax_deduction_fund_transfer_taxes", joinColumns = @JoinColumn(name = "tax_deduction_fund_transfer_id"))
	private List<TaxDetail> taxes;

	/**
	 * Status of the tax deduction transfer (Generated, Transferred, Failed, etc.)
	 */
	@Column(nullable = true)
	private String status;

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
