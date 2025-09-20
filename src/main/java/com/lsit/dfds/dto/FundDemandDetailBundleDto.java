// src/main/java/com/lsit/dfds/dto/FundDemandDetailBundleDto.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A response bundle with separate list objects */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDemandDetailBundleDto {
	private List<FundDemandResponseDto> demands;
	private List<WorkDetailDto> works;
	private List<VendorDetailDto> vendors;
	private List<TaxDetailDto> taxes;
}
