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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
//com.lsit.dfds.entity.DistrictLimitAllocation
@Table(name = "lsit_dfds_district_limit_allocation", uniqueConstraints = @UniqueConstraint(columnNames = {
		"district_id", "financial_year_id", "plan_type", "scheme_type_id" }))
public class DistrictLimitAllocation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	@Enumerated(EnumType.STRING)
	@Column(name = "plan_type", nullable = false)
	private PlanType planType;

// CHANGED: was nullable=false. For MLA/MLC/HADP this will be NULL.
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_type_id", nullable = true)
	private DemandCode schemeType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@Column(nullable = false)
	private Double allocatedLimit = 0.0;
	@Column(nullable = false)
	private Double utilizedLimit = 0.0;
	@Column(nullable = false)
	private Double remainingLimit = 0.0;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private Statuses status;

	@Column(nullable = true)
	private String remarks;
	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
