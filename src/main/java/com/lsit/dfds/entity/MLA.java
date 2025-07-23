package com.lsit.dfds.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

	@OneToOne
	@JoinColumn(name = "constituency_id", nullable = false)
	private Constituency constituency;

	@OneToMany(mappedBy = "recommendedByMla", cascade = CascadeType.ALL)
	private List<Work> recommendedWorks;
}
