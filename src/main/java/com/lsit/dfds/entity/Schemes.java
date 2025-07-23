package com.lsit.dfds.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.PlanType;
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
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_schemes")
public class Schemes {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String schemeName;

	@Column(nullable = true)
	private String schemeCode;

	@Column(nullable = true)
	private String crcCode;

	@Column(nullable = true)
	private String objectCode;

	@Column(nullable = true)
	private String objectName;

	@Column(nullable = true)
	private String majorHeadName;

	@Column(nullable = true)
	private String description;

	private Double allocatedBudget = 0.0;

	private Double estimatedBudget = 0.0;

	private Double revisedBudget = 0.0;

	private Double nextBudget = 0.0;

	@Enumerated
	@Column(nullable = true)
	private SchemeType schemeType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private PlanType planType;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sector_id")
	private Sector sector;

	@OneToMany(mappedBy = "scheme", cascade = CascadeType.ALL)
	private List<Work> works;

	@ManyToMany(mappedBy = "schemes")
	private List<District> districts;

}
