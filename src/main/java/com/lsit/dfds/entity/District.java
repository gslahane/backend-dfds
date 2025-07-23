package com.lsit.dfds.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_district")
public class District {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String districtName;

	@ManyToMany
	@JoinTable(name = "district_schemes", joinColumns = @JoinColumn(name = "district_id"), inverseJoinColumns = @JoinColumn(name = "scheme_id"))
	private List<Schemes> schemes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "division_id", nullable = false)
	private Division division;

	@OneToMany(mappedBy = "district", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Taluka> talukas;

	@OneToMany(mappedBy = "district", cascade = CascadeType.ALL)
	private List<Constituency> constituencies;

}
