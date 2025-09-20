package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_user_bank_account", uniqueConstraints = @UniqueConstraint(columnNames = { "user_id",
		"account_number", "ifsc" }))
public class UserBankAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "account_holder_name")
	private String accountHolderName;
	@Column(name = "account_number", nullable = false)
	private String accountNumber;
	@Column(name = "ifsc", nullable = false)
	private String ifsc;
	private String bankName;
	private String branch;
	private String accountType; // CA/SB
	// 👇 rename + map to non-reserved column name
	@Column(name = "is_primary", nullable = false)
	private Boolean isPrimary = false;

	@Enumerated(EnumType.STRING)
	private Statuses status = Statuses.ACTIVE;
	private Boolean verified = false;

	@CreationTimestamp
	private LocalDateTime createdAt;
	private String createdBy;
}
