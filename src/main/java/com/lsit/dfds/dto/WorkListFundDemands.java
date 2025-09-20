package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import com.lsit.dfds.enums.DemandStatuses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkListFundDemands {

	private Long id;
	private String demandId;
	private Double amount;
	private Double netPayable;
	private LocalDate demandDate;
	private Long financialYearId;
	private String financialYear; // optional
	private DemandStatuses status;
	private List<TaxDetailDto> taxes;
	private String remarks; // optional

}
