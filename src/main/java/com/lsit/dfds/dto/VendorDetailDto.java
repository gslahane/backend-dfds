// src/main/java/com/lsit/dfds/dto/VendorDetailDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Detailed Vendor info for the bundle */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorDetailDto {
	private Long vendorId;
	private String name;
	private String contactPerson;
	private String phone;
	private String email;

	private String gstNumber;
	private Boolean gstVerified;

	private String panNumber;

	private String address;
	private String city;
	private String state;
	private String pincode;

	// Bank
	private String bankAccountNumber;
	private String bankIfsc;
	private String bankName;
	private String bankBranch;

	private Boolean paymentEligible;
}
