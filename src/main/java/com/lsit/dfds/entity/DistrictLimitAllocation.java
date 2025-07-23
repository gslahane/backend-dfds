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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_district_limit_allocation")
public class DistrictLimitAllocation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	private Double allocatedLimit = 0.0; // Allocated by State Admin

	private Double utilizedLimit = 0.0; // Utilized by district while allocating to schemes

	private Double remainingLimit = 0.0; // = allocatedLimit - utilizedLimit

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status; // Active, Closed, etc.

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy; // typically State Admin username

	@CreationTimestamp
	private LocalDateTime createdAt;
}
