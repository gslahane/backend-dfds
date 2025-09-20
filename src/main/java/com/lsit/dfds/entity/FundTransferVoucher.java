// src/main/java/com/lsit/dfds/entity/FundTransferVoucher.java
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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_fund_transfer_voucher", uniqueConstraints = @UniqueConstraint(columnNames = "fund_demand_id"))
public class FundTransferVoucher {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// One voucher per demand (after vendor transfer)
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fund_demand_id", nullable = false, unique = true)
	private FundDemand fundDemand;

	@Column(nullable = false, unique = true)
	private String voucherNo; // e.g., VCH-<timestamp>

	// Snapshot fields (so future changes to vendor/work don’t alter the bill)
	@Column(nullable = false)
	private String demandId;

	private String schemeName;
	private String workName;
	private String workCode;

	private String vendorName;
	private String vendorAccountNumber;
	private String vendorIfsc;
	private String vendorBankName;

	private Double grossAmount = 0.0;
	private Double taxTotal = 0.0;
	private Double netPayable = 0.0;

	@ElementCollection
	@CollectionTable(name = "lsit_dfds_voucher_taxes", joinColumns = @JoinColumn(name = "voucher_id"))
	private List<TaxDetail> taxes; // tax snapshot

	// Optional cached PDF URL (e.g., S3) — can stay null if you render on demand
	private String pdfUrl;

	@Column(nullable = false)
	private String html = "";

	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}