// src/main/java/com/lsit/dfds/entity/MLCBudget.java
// (replace your current class with this additive version)
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
@Table(name = "lsit_dfds_mlc_budget")
public class MLCBudget {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mlc_id", nullable = false)
	private MLC mlc;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id", nullable = true) // make nullable if not always scheme-specific
	private Schemes scheme;

	// Policy info (for display): default 5 Cr/year
	private Double baseAnnualLimit = 5_00_00_000.0; // ₹5 Cr
	private Double carryForwardIn = 0.0; // optional

	// Effective allocation the State set (this is what matters operationally)
	private Double allocatedLimit = 0.0;
	private Double utilizedLimit = 0.0;

	public Double getRemainingLimit() {
		double alloc = allocatedLimit == null ? 0.0 : allocatedLimit;
		double used = utilizedLimit == null ? 0.0 : utilizedLimit;
		return alloc - used;
	}

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private Statuses status;

	private String remarks;
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
