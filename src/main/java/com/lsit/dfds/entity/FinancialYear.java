package com.lsit.dfds.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_Finacial_Year")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class FinancialYear {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String finacialYear; // e.g., "2024-25"

	// Nullable for now; backfill over time
	@Column(nullable = true)
	private Integer startYear; // e.g., 2024
}
