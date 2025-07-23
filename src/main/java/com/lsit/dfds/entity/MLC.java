package com.lsit.dfds.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
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

	private String category; // e.g., Graduate, Local Authority, etc.

	private String contactNumber;

	private String email;

	private String region; // Optional: Region or Division they belong to

	@ManyToMany
	@JoinTable(name = "mlc_district_access", joinColumns = @JoinColumn(name = "mlc_id"), inverseJoinColumns = @JoinColumn(name = "district_id"))
	private List<District> accessibleDistricts;

	@ManyToMany
	@JoinTable(name = "mlc_constituency_access", joinColumns = @JoinColumn(name = "mlc_id"), inverseJoinColumns = @JoinColumn(name = "constituency_id"))
	private List<Constituency> accessibleConstituencies;

	@OneToMany(mappedBy = "recommendedByMlc", cascade = CascadeType.ALL)
	private List<Work> recommendedWorks;
}
