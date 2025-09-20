package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.PanVerifyResult;
import com.lsit.dfds.dto.VendorDto;
import com.lsit.dfds.dto.VendorRegistrationResponse;
import com.lsit.dfds.dto.VendorRequestDto;
import com.lsit.dfds.dto.VerifyPanRequest;
import com.lsit.dfds.dto.VerifyPanResponse;
import com.lsit.dfds.service.VendorService;
import com.lsit.dfds.utils.JwtUtil;
import com.lsit.dfds.utils.PanVerificationTokenUtil;
import com.lsit.dfds.utils.VendorKycUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

	private final VendorService vendorService;
	private final VendorKycUtil vendorKycUtil;
	private final PanVerificationTokenUtil tokenUtil;
	private final JwtUtil jwtUtil;

	/** Step-1: verify PAN before filling the form. Returns a short-lived token. */
	@PostMapping("/pan/verify")
	public ResponseEntity<?> verifyPan(@Valid @RequestBody VerifyPanRequest req, HttpServletRequest httpReq) {
		try {
			// Optional: restrict to IA_ADMIN if you want
			// vendorService.validateIAAdmin(httpReq);

			PanVerifyResult pr = vendorKycUtil.verifyPanNow(req.getPan());
			if (!pr.isOk()) {
				return ResponseEntity.ok(new VerifyPanResponse(false, pr.getCode(), pr.getMessage(), null));
			}
			String token = tokenUtil.issueToken(req.getPan());
			return ResponseEntity.ok(new VerifyPanResponse(true, pr.getCode(), "PAN verified", token));
		} catch (Exception ex) {
			return ResponseEntity.badRequest().body(new VerifyPanResponse(false, "ERR", ex.getMessage(), null));
		}
	}

	/** Step-2: register vendor — requires token from /pan/verify */
	@PostMapping("/register")
	public ResponseEntity<?> registerVendor(@RequestBody VendorRequestDto dto, HttpServletRequest request) {
		try {
			VendorRegistrationResponse out = vendorService.registerVendor(dto, request);
			return ResponseEntity.ok(out);
		} catch (Exception ex) {
			return ResponseEntity.status(403).body(ex.getMessage());
		}
	}

	@PutMapping("/update/{id}")
	public ResponseEntity<?> updateVendor(@PathVariable Long id, @ModelAttribute VendorRequestDto dto,
			HttpServletRequest request) {
		try {
			return ResponseEntity.ok(vendorService.updateVendor(id, dto, request));
		} catch (Exception ex) {
			return ResponseEntity.status(403).body(ex.getMessage());
		}
	}

	@DeleteMapping("/delete/{id}")
	public ResponseEntity<?> deleteVendor(@PathVariable Long id, HttpServletRequest request) {
		try {
			vendorService.deleteVendor(id, request);
			return ResponseEntity.ok("Vendor deleted");
		} catch (Exception ex) {
			return ResponseEntity.status(403).body(ex.getMessage());
		}
	}

	@PutMapping("/activate/{id}")
	public ResponseEntity<?> activate(@PathVariable Long id, HttpServletRequest request) {
		return ResponseEntity.ok(vendorService.activateVendor(id, request));
	}

	@PutMapping("/deactivate/{id}")
	public ResponseEntity<?> deactivate(@PathVariable Long id, HttpServletRequest request) {
		return ResponseEntity.ok(vendorService.deactivateVendor(id, request));
	}

	@GetMapping
	public ResponseEntity<List<VendorDto>> getAllVendors(@RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long vendorId, @RequestParam(required = false) String search) {

		List<VendorDto> vendorDtos = vendorService.getVendorDetails(districtId, vendorId, search);
		return ResponseEntity.ok(vendorDtos);
	}
}
