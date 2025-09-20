package com.lsit.dfds.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.Statuses;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_hadp")
public class HADP {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id", nullable = true)
	private Schemes scheme;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "taluka_id", nullable = true)
	private Taluka taluka; // HADP is often Taluka-specific

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private Statuses status; // ACTIVE, CLOSED, etc.

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private HadpType type;

	@Column(nullable = true)
	private String description;

	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "hadp", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Work> works; // HADP can have multiple works

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = true)
	private District district;
}