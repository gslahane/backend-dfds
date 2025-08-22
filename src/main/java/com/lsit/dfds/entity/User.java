package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

//com.lsit.dfds.entity.User
@Data
@Entity
@Table(name = "lsit_dfds_user")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String fullname;

	@Column(nullable = false, unique = true)
	private String username; // required + unique

	private Long mobile = 0L;

	@Column(nullable = true)
	private String email;

	@Column(nullable = true)
	private String designation;

	@Column(nullable = false)
	private String password; // required

// (REMOVED) legacy bank columns: accountNumber, ifsc, bankName — use UserBankAccount

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "under_id")
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private User supervisor;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Roles role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Statuses status = Statuses.ACTIVE;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id")
	private District district;
}
