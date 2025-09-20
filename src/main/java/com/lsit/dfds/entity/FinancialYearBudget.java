package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.PlanType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_financial_year_budget")
public class FinancialYearBudget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private PlanType planType;

	private Double allocatedLimit = 0.0; // Allocated by State Admin

	private Double utilizedLimit = 0.0; // Utilized by district while allocating to schemes

	// Calculated field: remainingLimit is automatically calculated based on
	// allocated and utilized limits
	public Double getRemainingLimit() {
		return allocatedLimit - utilizedLimit;
	}

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status; // Active, Closed, etc.

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy; // typically State Admin username

	@CreationTimestamp
	private LocalDateTime createdAt; // auto-generated timestamp for creation
}
