package com.lsit.dfds.entity;

import java.util.List;

import com.lsit.dfds.enums.Statuses;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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
	private Integer term;

	@Enumerated(EnumType.STRING)
	private Statuses status;

	@OneToOne
	@JoinColumn(name = "constituency_id", nullable = false)
	private Constituency constituency;

	// ✅ MLA can recommend multiple schemes
	@ManyToMany
	@JoinTable(name = "mla_schemes", joinColumns = @JoinColumn(name = "mla_id"), inverseJoinColumns = @JoinColumn(name = "scheme_id"))
	private List<Schemes> schemes;

	@OneToMany(mappedBy = "recommendedByMla", cascade = CascadeType.ALL)
	private List<Work> recommendedWorks;

	// MLA.java
	@OneToOne
	@JoinColumn(name = "user_id", unique = true)
	private User user;
}
