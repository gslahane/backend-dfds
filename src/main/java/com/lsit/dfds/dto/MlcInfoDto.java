package com.lsit.dfds.dto;

import java.time.LocalDate;
import java.util.List;

import com.lsit.dfds.enums.Statuses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlcInfoDto {
	private Long id;
	private String mlcName;
	private String category;
	private String contactNumber;
	private String email;
	private Integer termNumber;
	private LocalDate tenureStartDate;
	private LocalDate tenureEndDate;
	private String tenurePeriod;
	private Statuses status;

//	private Boolean isNodal;

	private List<Long> accessibleDistrictIds;
	private List<String> accessibleDistrictNames;

	private List<Long> accessibleConstituencyIds;
	private List<String> accessibleConstituencyNames;

	private List<Long> schemeIds;
	private List<String> schemeNames;

	private Long userId; // linked login user (if any)
	private String username;

	private Long districtId;
	private String districtName;
}