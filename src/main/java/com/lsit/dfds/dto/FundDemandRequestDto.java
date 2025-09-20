package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class FundDemandRequestDto {
	private Long workId;
	private Long vendorId;
	private Double amount;
	private Double netPayable;
	private List<TaxDetailDto> taxes;
	private String remarks;
	private LocalDate demandDate;

}
