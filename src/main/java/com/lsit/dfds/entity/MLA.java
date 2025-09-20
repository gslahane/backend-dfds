package com.lsit.dfds.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_mla")
public class MLA {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String mlaName;
	private String party;
	private String contactNumber;
	private String email;

	@Enumerated(EnumType.STRING)
	private Statuses status;

	// ✅ New: store the latest activation/deactivation letter URL
	@Column(nullable = true)
	private String statusChangeLetterUrl;

	@OneToOne
	@JoinColumn(name = "constituency_id", nullable = false, unique = false)
	private Constituency constituency;
	// MLA.java
	/*
	 * @ManyToOne(fetch = FetchType.LAZY, optional = false)
	 *
	 * @JoinColumn(name = "default_scheme_id", nullable = false) private Schemes
	 * defaultScheme;
	 */

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "default_scheme_id", nullable = true)
	private Schemes defaultScheme;

	// ✅ MLA can recommend multiple schemes
//	@ManyToMany
//	@JoinTable(name = "mla_schemes", joinColumns = @JoinColumn(name = "mla_id"), inverseJoinColumns = @JoinColumn(name = "scheme_id"))
//	private List<Schemes> schemes;

	@OneToMany(mappedBy = "recommendedByMla", cascade = CascadeType.ALL)
	private List<Work> recommendedWorks;

	// MLA.java
	@OneToOne
	@JoinColumn(name = "user_id", unique = true)
	private User user;

	@OneToMany(mappedBy = "mla", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JsonIgnore // Prevent circular reference in serialization
	private List<MLATerm> mlaTerms;
}