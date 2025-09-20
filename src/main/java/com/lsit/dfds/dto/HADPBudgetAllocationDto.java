package com.lsit.dfds.dto;

import lombok.Data;

//@Data
//public class HADPBudgetAllocationDto {
//	private Long hadpTalukaId; // HADP record
//	private Long districtId; // District (now derived from HADP)
//	private Long financialYearId; // Financial Year
//	private Double allocatedLimit; // Allocation
//	private String remarks;
//}

@Data
public class HADPBudgetAllocationDto {
    private Long talukaId;
    private Long financialYearId;
    private Long schemeId;
    private Double allocatedLimit;
    private String remarks;
}

