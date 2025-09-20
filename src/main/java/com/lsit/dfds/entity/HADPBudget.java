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
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_hadp_budget")
public class HADPBudget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// ✅ Link to HADP
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hadp_id", nullable = false)
	private HADP hadp;

	// ✅ Linked Scheme
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id", nullable = false)
	private Schemes scheme;

	// ✅ Linked Taluka (Optional)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "taluka_id", nullable = true)
	private Taluka taluka;

	// ✅ Linked District (Optional)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = true)
	private District district;

	// ✅ Financial Year
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	// ✅ Budget details
	@Column(nullable = false)
	private Double allocatedLimit = 0.0;

	@Column(nullable = false)
	private Double utilizedLimit = 0.0;

	@Column(nullable = false)
	private Double remainingLimit = 0.0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Statuses status; // ACTIVE / CLOSED

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}