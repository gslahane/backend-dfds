package com.lsit.dfds.dto;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class AssignWorkRequestDto {

	private Long schemeId; // Selected Scheme
	private PlanType planType; // ✅ Required: DAP, MLA, MLC, HADP
	private Long financialYearId; // Financial Year of the work
	private String workTitle; // Work name/title
	private Double aaAmount; // Admin Approved Amount
	private MultipartFile aaLetterFile; // Upload AA letter

	// ✅ For MLA plan type
	private Long mlaId;

	// ✅ For MLC plan type
	private Long mlcId;
	private Long districtId; // District selection for MLC nodal/non-nodal work

	// ✅ For HADP plan type
	private Long talukaId;

	// ✅ Implementing Agency (IA User)
	private Long iaUserId; // Selected IA user
}
