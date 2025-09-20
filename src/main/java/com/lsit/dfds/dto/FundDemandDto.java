package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundDemandDto {
	private Long demandId;
	private Double amount;
	private Double netPayable;
	private String status;
	private LocalDate demandDate;
}
