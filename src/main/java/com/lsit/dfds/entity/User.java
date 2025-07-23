package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;

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
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String username;

	@Column(nullable = true)
	private String agencyCode;

	private Long mobile = 0L;

	@Column(nullable = true)
	private String email;

	@Column(nullable = true)
	private String designation;

	@Column(nullable = true)
	private String password;

	@Column(nullable = true)
	private String accountNumber;

	@Column(nullable = true)
	private String ifsc;

	@Column(nullable = true)
	private String bankName;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "under_id", referencedColumnName = "id", nullable = true)
	private User supervisor; // replaces underId for clean hierarchy

	@Enumerated
	@Column(nullable = true)
	private Roles role;

	@Column(nullable = true)
	@Enumerated(EnumType.STRING)
	private Statuses status;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id")
	private District district;

}
