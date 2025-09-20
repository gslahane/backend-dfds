// src/main/java/com/lsit/dfds/controller/MlaAdminController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlaInfoDto;
import com.lsit.dfds.dto.MlaListFilterDto;
import com.lsit.dfds.dto.MlaStatusUpdateDto;
import com.lsit.dfds.dto.MlaUpdateDto;
import com.lsit.dfds.service.MlaAdminService;
import com.lsit.dfds.utils.JwtUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/mlas")
@RequiredArgsConstructor
public class MlaAdminController {

	private final MlaAdminService service;
	private final JwtUtil jwtUtil;

	@GetMapping
	public ResponseEntity<List<MlaInfoDto>> list(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long constituencyId,
			@RequestParam(required = false) String status, @RequestParam(required = false) String search) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		MlaListFilterDto filter = new MlaListFilterDto();
		filter.setDistrictId(districtId);
		filter.setConstituencyId(constituencyId);
		filter.setStatus(status);
		filter.setSearch(search);
		return ResponseEntity.ok(service.list(username, filter));
	}

	@GetMapping("/{mlaId}")
	public ResponseEntity<MlaInfoDto> getOne(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long mlaId) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(service.getOne(username, mlaId));
	}

	@PutMapping("/{mlaId}")
	public ResponseEntity<MlaInfoDto> update(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long mlaId, @RequestBody MlaUpdateDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(service.update(username, mlaId, dto));
	}

	// PATCH /api/mlas/{mlaId}/status with multipart form:
	// - payload: MlaStatusUpdateDto (JSON)
	// - letter: file (pdf/doc/docx/jpg/png) required by policy
	@PatchMapping(value = "/{mlaId}/status", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<MlaInfoDto> updateStatus(@RequestHeader("Authorization") String authHeader,
			@PathVariable Long mlaId, @RequestPart("payload") @Valid MlaStatusUpdateDto dto,
			@RequestPart(name = "letter", required = false) MultipartFile letter) {

		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(service.updateStatus(username, mlaId, dto, letter));
	}
}
