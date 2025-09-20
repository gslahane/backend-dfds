package com.lsit.dfds.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_vendor")
public class Vendor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String name;

	@Column(nullable = true)
	private String aadhaarNumber;

	@Column(nullable = true)
	private String gstNumber;

	@Column(nullable = true)
	private String panNumber;

	@Column(nullable = true)
	private String contactPerson;

	@Column(nullable = true)
	private String phone;

	@Column(nullable = true)
	private String email;

	@Column(nullable = true)
	private String address;

	@Column(nullable = true)
	private String city;

	@Column(nullable = true)
	private String state;

	@Column(nullable = true)
	private String pincode;

	@Column(nullable = true)
	private LocalDate registrationDate;

	@Column(nullable = true)
	private String verificationStatus;

	private Boolean aadhaarVerified = false;

	private Boolean gstVerified = false;

	@Column(nullable = true)
	private String type; // Individual, Company, etc.

	@Column(nullable = true)
	private String category; // Construction, Electrical, etc.

	@Column(nullable = true)
	private String status; // Active, Inactive, etc.

	private Double performanceRating = 0.0;

	private Double creditLimit = 0.0;

	@Column(nullable = true)
	private String paymentTerms; // Net 30, etc.

	@Column(nullable = true)
	private String bankAccountNumber;

	@Column(nullable = true)
	private String bankIfsc;

	@Column(nullable = true)
	private String bankName;

	@Column(nullable = true)
	private String bankBranch;

	@ElementCollection
	private List<String> documentUrls; // Stores URLs only

	private Boolean paymentEligible = false;

	@Column(nullable = true)
	private LocalDate lastUpdated;

	@Column(nullable = true)
	private String updatedBy;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy; // 👈 Add this field

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", unique = true)
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "vendor" }) // Ignore circular references
	private User user; // ⬅️ link login user to vendor (optional, unique)

}
