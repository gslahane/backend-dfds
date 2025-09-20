package com.lsit.dfds.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsit.dfds.dto.AssignVendorRequestDto;
import com.lsit.dfds.dto.PagedResponse;
import com.lsit.dfds.dto.WorkRowDto;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.service.WorkVendorMappService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/work-vendor-mapping")
@RequiredArgsConstructor
public class WorkVendorMappController {

	private final WorkVendorMappService workVendorMappService;
	private final JwtUtil jwtUtil;
	private final ObjectMapper objectMapper; // inject via constructor

	@GetMapping("/works")
	public ResponseEntity<?> getWorks(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) String financialYear, @RequestParam(required = false) Long schemeId,
			@RequestParam(required = false) String status) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(workVendorMappService.getWorksForIAUser(username, financialYear, schemeId, status));
	}

	@GetMapping("/vendors")
	public ResponseEntity<?> getVendors(@RequestHeader("Authorization") String authHeader) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(workVendorMappService.getVendorsForIA(username));
	}

	@PostMapping(value = "/assign-vendor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> assignVendor(@RequestHeader("Authorization") String authHeader,
			@RequestPart("payload") String payload, // JSON string
			@RequestPart(name = "workOrderFile", required = false) MultipartFile workOrderFile // optional file
	) throws Exception {

		String username = jwtUtil.extractUsername(authHeader.substring(7));

		// If some clients send text/plain, normalize quotes etc. if needed
		String json = cleanMultipartJson(payload); // optional helper; or just use `payload` directly
		AssignVendorRequestDto dto = objectMapper.readValue(json, AssignVendorRequestDto.class);

		Work saved = workVendorMappService.assignVendorToWork(dto, username, workOrderFile);
		return ResponseEntity.ok(saved); // or wrap in your WorkDto/GenericResponseDto if you prefer
	}

	// Optional: same helper you already use elsewhere
	private String cleanMultipartJson(String raw) {
		return raw != null ? raw.trim() : raw;
	}

	@GetMapping("/counts")
	public ResponseEntity<?> getDashboardCounts(@RequestHeader("Authorization") String authHeader) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(workVendorMappService.getDashboardCounts(username));
	}

	/**
	 * Returns ONLY AA_APPROVED works filtered by planType (MLA/MLC/HADP). Optional
	 * filters: financialYearId, schemeId. Pagination: page, size.
	 */
	@GetMapping("/aa-approved-works")
	public ResponseEntity<PagedResponse<WorkRowDto>> getAaApprovedWorks(
			@RequestHeader("Authorization") String authHeader, @RequestParam PlanType planType, // MLA / MLC / HADP
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long schemeId,
			@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "50") Integer size) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		var res = workVendorMappService.getAaApprovedWorks(username, planType, financialYearId, schemeId, page, size);
		return ResponseEntity.ok(res);
	}

}
