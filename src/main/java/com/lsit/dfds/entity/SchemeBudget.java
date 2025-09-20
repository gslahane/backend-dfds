package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.Statuses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_scheme_budget")
public class SchemeBudget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_type_id", nullable = true)
	private DemandCode schemeType; // Optional

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear; // Many-to-one relationship

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id", nullable = false)
	private Schemes scheme; // One-to-one relationship with Scheme

	private Double allocatedLimit = 0.0; // Allocated by State Admin

	private Double utilizedLimit = 0.0; // Utilized by district while allocating to schemes

	private Double remainingLimit = 0.0; // Calculated as allocatedLimit - utilizedLimit

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status; // Active, Closed, etc.

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy; // Typically State Admin username

	@CreationTimestamp
	private LocalDateTime createdAt; // Auto-generated timestamp
}
