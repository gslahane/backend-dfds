package com.lsit.dfds.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendorRegistrationResponse {
	private Long vendorId;
	private String vendorName;
	private String username;
	private String tempPassword; // returned once to IA for sharing with vendor
	private String message;
}
