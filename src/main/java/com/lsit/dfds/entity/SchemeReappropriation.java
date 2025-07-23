package com.lsit.dfds.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.ReappropriationStatuses;
import com.lsit.dfds.enums.SchemeType;

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
@Table(name = "lsit_dfds_scheme_reappropriation")
public class SchemeReappropriation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SchemeType schemeType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "target_scheme_id", nullable = false)
	private Schemes targetScheme;

	private Double totalTransferAmount = 0.0;

	@Column(nullable = false)
	private LocalDate reappropriationDate;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private ReappropriationStatuses status; // Pending, Approved, Rejected

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = true)
	private String createdBy;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "reappropriation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SchemeReappropriationSource> sourceSchemes;
}
