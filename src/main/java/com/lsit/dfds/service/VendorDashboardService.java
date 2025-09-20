// src/main/java/com/lsit/dfds/service/VendorDashboardService.java
package com.lsit.dfds.service;

import com.lsit.dfds.dto.VendorDashboardResponseDto;

public interface VendorDashboardService {
	VendorDashboardResponseDto getDashboard(String username, Long vendorId, // ignored when caller is vendor
			Long financialYearId, Long districtId, Long schemeId, String status, // DemandStatuses name
			String search);
}
