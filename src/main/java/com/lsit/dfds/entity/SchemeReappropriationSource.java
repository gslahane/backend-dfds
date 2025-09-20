package com.lsit.dfds.entity;

import jakarta.persistence.Entity;
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
@Table(name = "lsit_dfds_scheme_reappropriation_source")
public class SchemeReappropriationSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reappropriation_id", nullable = false)
	private SchemeReappropriation reappropriation;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "source_scheme_id", nullable = false)
	private Schemes sourceScheme;

	private Double transferAmount = 0.0;

	private Double sourcePreviousBalance = 0.0;

	private Double sourceUpdatedBalance = 0.0;
}
