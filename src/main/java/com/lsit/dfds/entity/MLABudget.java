package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.Statuses;

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
@Table(name = "lsit_dfds_mla_budget")
public class MLABudget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mla_id", nullable = false)
	private MLA mla;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "constituency_id", nullable = false)
	private Constituency constituency;

	// NEW: split for auditability
	private Double baseAnnualLimit = 0.0; // usually 5_00_00_000.0
	private Double carryForwardIn = 0.0; // previous FY remaining (same term)

	private Double allocatedLimit = 0.0; // = base + carryForwardIn
	private Double utilizedLimit = 0.0;

	public Double getRemainingLimit() {
		double alloc = allocatedLimit == null ? 0.0 : allocatedLimit;
		double used = utilizedLimit == null ? 0.0 : utilizedLimit;
		return alloc - used;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id")
	private Schemes scheme; // optional

	@Enumerated(EnumType.STRING)
	private Statuses status;

	private String remarks;
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
