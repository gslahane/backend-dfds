package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class VendorDto {
	private Long id;
	private String name;
	private String contactPerson;
	private String phone;
	private String email;
	private String address;
	private String city;
	private String state;
	private String pincode;
	private LocalDate registrationDate;
	private String verificationStatus;
	private Boolean aadhaarVerified;
	private Boolean gstVerified;
	private String type; // Individual, Company, etc.
	private String category; // Construction, Electrical, etc.
	private String status; // Active, Inactive, etc.
	private Double performanceRating;
	private Double creditLimit;
	private String paymentTerms;
	private List<String> documentUrls; // Stores URLs only
	private Boolean paymentEligible;
	private LocalDate lastUpdated;
	private String updatedBy;
	private LocalDateTime createdAt;
	private String createdBy;
	private String gstNumber;
	private String panNumber;
	private String aadhaarNumber;
	private String bankName;
	private String bankAccountNumber;
	private String bankIfsc;
	private String bankBranch;

	// Add fields related to the User entity (implementing agency details)
	private String username;
	private Long mobile;
}
