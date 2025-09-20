package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.SpillStatuses;

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
@Table(name = "lsit_dfds_work_spill_request")
public class WorkSpillRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "work_id", nullable = false)
	private Work work;

	// Keep both for sanity
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_scheme_id", nullable = false)
	private Schemes fromScheme;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_scheme_id", nullable = false)
	private Schemes toScheme; // next-FY scheme (same demand code/plan)

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_fy_id", nullable = false)
	private FinancialYear fromFy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_fy_id", nullable = false)
	private FinancialYear toFy;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PlanType planType; // mirror of scheme.planType at request time

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district; // work’s district

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "demand_code_id", nullable = false)
	private DemandCode demandCode; // fromScheme.getSchemeType()

	@Column(nullable = false)
	private Double carryForwardAmount = 0.0; // typically work.balanceAmount now

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SpillStatuses status = SpillStatuses.PENDING;

	@Column(nullable = true)
	private String remarks;
	@Column(nullable = true)
	private String createdBy;
	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String decidedBy;
	@Column(nullable = true)
	private LocalDateTime decidedAt;
}
