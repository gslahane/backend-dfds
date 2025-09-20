// src/main/java/com/lsit/dfds/dto/VendorLiteDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorLiteDto {
	private Long id;
	private String name;
	private String gstNumber;
	private String panNumber;
	private String contactPerson;
	private String phone;
	private String email;
}
