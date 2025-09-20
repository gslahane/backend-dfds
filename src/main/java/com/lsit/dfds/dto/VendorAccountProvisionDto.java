// com.lsit.dfds.dto.VendorAccountProvisionDto
package com.lsit.dfds.dto;

import lombok.Data;

@Data
public class VendorAccountProvisionDto {
	private Long vendorId; // required
	private String username; // required, unique
	private String password; // required
	private String email; // optional
	private Long mobile; // optional
	private String fullName; // optional (defaults to vendor.name)
}
