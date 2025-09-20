// src/main/java/com/lsit/dfds/dto/WorkRecommendDto.java
package com.lsit.dfds.dto;

import com.lsit.dfds.enums.PlanType;

import lombok.Data;

@Data
public class WorkRecommendDto {
	private String workName;
	private String description;
	private Double estimatedAmount; // proposal amount (not AA yet)
	private Long constituencyId; // required for MLA
	private Long targetDistrictId; // required for MLC (where work will be executed)
	private Long talukaId; // required for HADP
	private Long schemeId; // optional override; else use default per role/plan
	private PlanType planType; // MLA, MLC, HADP, DAP (DAP will be done by district)
	private Boolean nodal; // optional (for MLC); computed if null
}