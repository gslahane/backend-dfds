package com.lsit.dfds.dto;

import java.util.List;

import lombok.Data;

@Data
public class MlcUpdateDto {
	private String mlcName;
	private String category; // Graduate, Local Authority, etc.
	private String contactNumber;
	private String email;
	private Integer term;
	private Boolean isNodal; // optional toggle

	// optional mapping updates
	private List<Long> accessibleDistrictIds;
	private List<Long> accessibleConstituencyIds;
	private List<Long> schemeIds; // if you map MLC ↔ schemes
}
