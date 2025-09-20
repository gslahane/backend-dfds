// src/main/java/com/lsit/dfds/dto/WorkRowDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkRowDto {
	private Long workId;
	private String workCode;
	private String workName;

	private Long financialYearId;
	private String financialYear;

	private PlanType planType;

	private Long schemeId;
	private String schemeName;

	private Long districtId;
	private String districtName;

	// MLA-specific (present when planType = MLA)
	private Long mlaId;
	private String mlaName;
	private Long constituencyId;
	private String constituencyName;

	// MLC-specific (present when planType = MLC)
	private Long mlcId;
	private String mlcName;

	// HADP-specific (present when planType = HADP)
	private Long hadpId;
	private Long talukaId;
	private String talukaName;

	// IA / Vendor context
	private Long iaUserId;
	private String iaFullname;
	private Long vendorId;
	private String vendorName;

	// AA context
	private Double adminApprovedAmount;
//    private Double balanceAmount;
	private LocalDate sanctionDate;
	private String adminApprovalLetterUrl;

	private WorkStatuses status; // should always be AA_APPROVED in this endpoint
}
