package com.lsit.dfds.service;

//com.lsit.dfds.service.VendorAuthService

import com.lsit.dfds.dto.VendorAccountProvisionDto;
import com.lsit.dfds.entity.User;

public interface VendorAuthService {
	User provisionVendorUser(VendorAccountProvisionDto dto, String createdBy);

	void revokeVendorUser(Long vendorId, String requestedBy); // optional
}
