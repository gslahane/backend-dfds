package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.WorkStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Comprehensive DTO for work details with status, vendor details, budget
 * details, and utilization details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkDetailsWithStatusDto {

	// ==================== WORK BASIC DETAILS ====================
	private Long workId;
	private String workName;
	private String workCode;
	private String description;
	private String remark;
	private WorkStatuses status;
	private LocalDate workStartDate;
	private LocalDate workEndDate;
	private LocalDate sanctionDate;
	private LocalDateTime createdAt;
	private String createdBy;

	private Double recommendedAmount;
	private Double districtApprovedAmount;

	// ==================== BUDGET DETAILS ====================
	private Double grossAmount; // Original recommended amount
	private Double adminApprovedAmount; // Final approved amount
	private Double balanceAmount; // Remaining balance
	private Double workPortionTax;
	private Double workPortionAmount;

	// ==================== SCHEME & FINANCIAL YEAR ====================
	private Long schemeId;
	private String schemeName;
	private String planType;
	private Long financialYearId;
	private String financialYear;

	// ==================== LOCATION DETAILS ====================
	private Long districtId;
	private String districtName;
	private Long constituencyId;
	private String constituencyName;

	// ==================== RECOMMENDATION DETAILS ====================
	private Long mlaId;
	private String mlaName;
	private String mlaParty;
	private Long mlcId;
	private String mlcName;
	private String mlcCategory;
	private Boolean isNodalWork; // For MLC works

	// ==================== IMPLEMENTING AGENCY ====================
	private Long implementingAgencyId;
	private String implementingAgencyName;
	private String implementingAgencyUsername;
	private String implementingAgencyMobile;

	// ==================== VENDOR DETAILS ====================
	private VendorDetailsDto vendor;

	// ==================== UTILIZATION DETAILS ====================
	private Double totalFundDisbursed; // Total amount disbursed through fund demands
	private Double totalFundDemanded; // Total amount demanded
	private Double utilizationProgressPercentage; // (Fund Disbursed / Admin Approved Amount) * 100
	private Integer totalDemandsCount;
	private Integer approvedDemandsCount;
	private Integer pendingDemandsCount;
	private Integer rejectedDemandsCount;

	// ==================== FUND DEMANDS ====================
	private List<FundDemandSummaryDto> fundDemands;

	// ==================== DOCUMENTS ====================
	private String recommendationLetterUrl;
	private String adminApprovedLetterUrl;
	private String workOrderLetterUrl;

	// ==================== NESTED DTOs ====================

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class VendorDetailsDto {
		private Long vendorId;
		private String vendorName;
		private String contactPerson;
		private String phone;
		private String email;
		private String address;
		private String city;
		private String state;
		private String pincode;
		private String gstNumber;
		private String panNumber;
		private String type; // Individual, Company, etc.
		private String category; // Construction, Electrical, etc.
		private String status; // Active, Inactive, etc.
		private Double performanceRating;
		private LocalDate registrationDate;
		private Boolean aadhaarVerified;
		private Boolean gstVerified;
		private Boolean paymentEligible;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class FundDemandSummaryDto {
		private Long demandId;
		private String demandCode;
		private Double amount;
		private Double netPayable;
		private LocalDate demandDate;
		private String status;
		private LocalDateTime createdAt;
		private String createdBy;
		private Double fundDisbursed; // Amount actually transferred
		private String transferStatus; // TRANSFERRED, PENDING, etc.
	}
}
