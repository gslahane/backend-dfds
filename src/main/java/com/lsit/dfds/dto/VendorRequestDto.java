package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class VendorRequestDto {

	private String name;

	private String aadhaarNumber;

	private String gstNumber;

	private String panNumber;

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

	private String bankAccountNumber;

	private String bankIfsc;

	private String bankName;

	private String bankBranch;

//	private List<String> documentUrls;

	private Boolean paymentEligible;

	private LocalDate lastUpdated;

	private String updatedBy;

	private String desiredUsername; // optional (fallbacks to email/phone if not given)
	private String desiredPassword;
//	private List<MultipartFile> documentFiles;

	// NEW: proof that backend already verified this PAN in this session
	private String panVerificationToken;

}
