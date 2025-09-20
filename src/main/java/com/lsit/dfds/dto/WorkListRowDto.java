package com.lsit.dfds.dto;

//src/main/java/com/lsit/dfds/dto/WorkListRowDto.java

import java.time.LocalDate;
import java.util.List;

import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkListRowDto {
	// Work identity
	private Long workId;
	private String workCode;
	private String workName;

	// Plan & scheme
	private PlanType planType;
	private Long schemeId;
	private String schemeName;

	// Financial Year
	private Long financialYearId;
	private String financialYear;

	// District
	private Long districtId;
	private String districtName;

	// MLA plan extras
	private Long mlaId;
	private String mlaName;
	private Long constituencyId;
	private String constituencyName;

	// MLC plan extras
	private Long mlcId;
	private String mlcName;

	// HADP plan extras
	private Long hadpId;
	private Long talukaId;
	private String talukaName;

	// IA & Vendor
	private Long iaUserId;
	private String iaFullname;
	private VendorLiteDto vendor;

	// Work financials & docs
	private Double adminApprovedAmount;
	private Double workPortionAmount;
	private Double workPortionTax;
	private Double grossAmount;
	private Double balanceAmount;
	private LocalDate sanctionDate;
	private String workOrderLetterUrl;

	// Status
	private WorkStatuses status;

	// Demands (all past + present)
	private List<WorkListFundDemands> demands;
}
