package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.Statuses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_tax")
public class Tax {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String taxName;

	@Column(nullable = true)
	private String taxType; // e.g., GST, VAT, Service Tax

	private Double taxPercentage = 0.0;

	private Double fixedAmount = 0.0;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status; // Active, Inactive

	@Column(nullable = true)
	private String remarks;

	@Column(nullable = false, unique = true)
	private String code; // e.g., CGST, SGST

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;
}
