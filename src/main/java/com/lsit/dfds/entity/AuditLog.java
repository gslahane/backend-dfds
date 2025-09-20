package com.lsit.dfds.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_audit_log", indexes = { @Index(name = "idx_audit_actor", columnList = "actorUsername"),
		@Index(name = "idx_audit_entity", columnList = "entityType,entityId") })
public class AuditLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String actorUsername; // who performed the action (can be null for system jobs)

	@Column(nullable = false)
	private String action; // e.g. REGISTER_USER, LOGIN_SUCCESS, LOGIN_FAILED

	@Column(nullable = true)
	private String entityType; // e.g. "User", "MLA"; optional

	@Column(nullable = true)
	private Long entityId; // optional

	@Column(nullable = true, length = 4000)
	private String detailsJson; // optional JSON payload

	@CreationTimestamp
	private LocalDateTime createdAt;
}
