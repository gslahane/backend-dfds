package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.VendorDto;
import com.lsit.dfds.dto.VendorRegistrationResponse;
import com.lsit.dfds.dto.VendorRequestDto;
import com.lsit.dfds.entity.Vendor;

import jakarta.servlet.http.HttpServletRequest;

public interface VendorService {
	VendorRegistrationResponse registerVendor(VendorRequestDto dto, HttpServletRequest request);

	Vendor updateVendor(Long id, VendorRequestDto dto, HttpServletRequest request);

	void deleteVendor(Long id, HttpServletRequest request);

	Vendor activateVendor(Long id, HttpServletRequest request);

	Vendor deactivateVendor(Long id, HttpServletRequest request);

	List<Vendor> getAllVendors();

	List<VendorDto> getVendorDetails(Long districtId, Long vendorId, String search);
}
