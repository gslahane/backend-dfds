package com.lsit.dfds.entity;

import java.util.List;

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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_mlc")
public class MLC {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String mlcName;

	private String category; // Graduate, Local Authority, etc.

	private String contactNumber;

	private String email;

	private Integer term;

	private String region; // Optional

	@Enumerated(EnumType.STRING)
	private Statuses status;

	// ✅ New: latest activation/deactivation letter URL
	@Column(nullable = true)
	private String statusChangeLetterUrl;

	@ManyToMany
	@JoinTable(name = "mlc_district_access", joinColumns = @JoinColumn(name = "mlc_id"), inverseJoinColumns = @JoinColumn(name = "district_id"))
	private List<District> accessibleDistricts;

	@ManyToMany
	@JoinTable(name = "mlc_constituency_access", joinColumns = @JoinColumn(name = "mlc_id"), inverseJoinColumns = @JoinColumn(name = "constituency_id"))
	private List<Constituency> accessibleConstituencies;

	// ✅ Add Scheme Mapping
	@ManyToMany
	@JoinTable(name = "mlc_schemes", joinColumns = @JoinColumn(name = "mlc_id"), inverseJoinColumns = @JoinColumn(name = "scheme_id"))
	private List<Schemes> schemes;

	@OneToMany(mappedBy = "recommendedByMlc", cascade = CascadeType.ALL)
	private List<Work> recommendedWorks;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ia_user_id", nullable = true)
	private User implementingAgency;

	@Column(nullable = true)
	private Boolean isNodal = true;

	// MLC.java
	@OneToOne
	@JoinColumn(name = "user_id", unique = true)
	private User user;
}
