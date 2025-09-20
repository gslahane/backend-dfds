package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkResponseDto {
	private Long id;
	private String mlaMlcName;
	private String iaName;
	private String workTitle;
	private Double aaAmount;
	private String aaLetterUrl;
}
