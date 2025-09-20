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
@Table(name = "lsit_dfds_district_scheme_allocation")
public class DistrictSchemeAllocation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id", nullable = false)
	private Schemes scheme;

	/**
	 * Total limit allocated to this scheme for this district
	 */
	private Double allocatedLimit = 0.0;

	/**
	 * Total amount utilized from the allocated limit
	 */
	private Double utilizedLimit = 0.0;

	/**
	 * Remaining limit = allocatedLimit - utilizedLimit
	 */
	private Double remainingLimit = 0.0;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status; // Active, Closed, etc.

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;
}
