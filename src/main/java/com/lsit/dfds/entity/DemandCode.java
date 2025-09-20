package com.lsit.dfds.entity;

import java.util.List;

import com.lsit.dfds.enums.BaseSchemeType;

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
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_demand_code")
public class DemandCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// e.g. "O26 - Revenue", "O27 - Capital", "Debt"
	private String schemeType;

	@Enumerated(EnumType.STRING)
	@Column(name = "base_type")
	private BaseSchemeType baseType; // (Renamed from 'Type')

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = false)
	private FinancialYear financialYear;

	@ManyToMany
	@JoinTable(name = "district_scheme_types", joinColumns = @JoinColumn(name = "scheme_type_id"), inverseJoinColumns = @JoinColumn(name = "district_id"))
	private List<District> districts;

	// 🔁 change this from @ManyToMany to @OneToMany
	@OneToMany(mappedBy = "schemeType", cascade = CascadeType.ALL)
	private List<Schemes> schemes;
}
