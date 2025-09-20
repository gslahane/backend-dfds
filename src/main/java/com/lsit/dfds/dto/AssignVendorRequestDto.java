package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class AssignVendorRequestDto {
	private Long workId;
	private Long vendorId;
	private Double workPortionAmount;
	private Double grossAmount;
	private String workOrderLetterUrl; // URL of the uploaded work order document
	private List<TaxDetailDto> taxes; // Selected taxes
}
