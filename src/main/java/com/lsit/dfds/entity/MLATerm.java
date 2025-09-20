package com.lsit.dfds.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "lsit_dfds_mla_term", uniqueConstraints = @UniqueConstraint(columnNames = { "mla_id", "start_fy_id",
		"end_fy_id" }))
public class MLATerm {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mla_id", nullable = false)
	private MLA mla;

	@Column(nullable = false)
	private Boolean active = true;

	@Column(name = "tenure_start_date", nullable = false)
	private LocalDate tenureStartDate;

	@Column(name = "tenure_period", nullable = true)
	private String tenurePeriod;

	@Column(name = "tenure_end_date", nullable = false)
	private LocalDate tenureEndDate;

	@Column(name = "term_number", nullable = false)
	private Integer termNumber;
}