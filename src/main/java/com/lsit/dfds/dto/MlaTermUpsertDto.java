package com.lsit.dfds.dto;

import java.time.LocalDate;

import lombok.Data;

@Data
public class MlaTermUpsertDto {
	private Long mlaId;
	private Long startFinancialYearId; // inclusive
	private Long endFinancialYearId; // inclusive (5th year)
	private Boolean active = true; // default true
	private LocalDate tenureStartDate;
	private LocalDate tenureEndDate;
	private String tenurePeriod;
	private Integer termNumber;
}