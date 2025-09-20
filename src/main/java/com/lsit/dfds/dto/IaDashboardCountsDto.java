// src/main/java/com/lsit/dfds/dto/IaDashboardCountsDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IaDashboardCountsDto {
	private long totalAaWorks; // Works with AA (adminApprovedAmount > 0)
	private double totalAdminApprovedAmount; // Sum of AA amounts
	private long totalWorksAssignedWithVendors;// Works with vendor assigned (any status)
	private long pendingWorkVendorRegistration;// AA works without vendor
}
