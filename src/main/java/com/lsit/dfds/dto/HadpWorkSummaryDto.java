// src/main/java/com/lsit/dfds/dto/HadpWorkSummaryDto.java
package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HadpWorkSummaryDto {
	private Long id;
	private String workName;
	private String workCode;
	private WorkStatuses status;
	private Double recommendedAmount;
	private Double districtApprovedAmount;
	private Double adminApprovedAmount;
	private Double balanceAmount;
	private LocalDate sanctionDate;
	private LocalDateTime createdAt;
}
