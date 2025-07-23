package com.lsit.dfds.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.DemandStatuses;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_fund_demand")
public class FundDemand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String demandId; // e.g., DM-123456

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "work_id", nullable = false)
	private Work work;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vendor_id", nullable = false)
	private Vendor vendor;

	private Double amount = 0.0; // Gross demand amount

	private Double netPayable = 0.0; // After tax deductions

	@ElementCollection
	@CollectionTable(name = "lsit_dfds_fund_demand_taxes", joinColumns = @JoinColumn(name = "fund_demand_id"))
	private List<TaxDetail> taxes; // Stores tax name, percentage, and amount

	@Column(nullable = false)
	private LocalDate demandDate;

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private DemandStatuses status; // Pending, Approved, Rejected

	@Column(nullable = true)
	private Boolean isDelete = false;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;
}
