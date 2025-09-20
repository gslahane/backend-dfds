// com.lsit.dfds.controller.VendorAuthController
package com.lsit.dfds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.VendorAccountProvisionDto;
import com.lsit.dfds.service.VendorAuthService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vendor-auth")
@RequiredArgsConstructor
public class VendorAuthController {

	private final VendorAuthService vendorAuthService;
	private final JwtUtil jwt;

	@PostMapping("/provision")
	public ResponseEntity<?> provision(@RequestHeader("Authorization") String authHeader,
			@RequestBody VendorAccountProvisionDto dto) {
		String createdBy = jwt.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(vendorAuthService.provisionVendorUser(dto, createdBy));
	}

	@PostMapping("/revoke/{vendorId}")
	public ResponseEntity<?> revoke(@RequestHeader("Authorization") String authHeader, @PathVariable Long vendorId) {
		String requestedBy = jwt.extractUsername(authHeader.substring(7));
		vendorAuthService.revokeVendorUser(vendorId, requestedBy);
		return ResponseEntity.ok("Vendor login revoked");
	}
}
