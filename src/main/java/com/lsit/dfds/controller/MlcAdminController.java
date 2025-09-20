// src/main/java/com/lsit/dfds/controller/MlcAdminController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlcInfoDto;
import com.lsit.dfds.dto.MlcListFilterDto;
import com.lsit.dfds.dto.MlcStatusUpdateDto;
import com.lsit.dfds.dto.MlcUpdateDto;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.service.MlcAdminService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/mlc")
@RequiredArgsConstructor
public class MlcAdminController {

	private final MlcAdminService service;
	private final JwtUtil jwt;

	// List + search/filter
	@GetMapping
	public ResponseEntity<List<MlcInfoDto>> list(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) String status, // ACTIVE /
																											// INACTIVE
																											// / CLOSED
																											// etc.
			@RequestParam(required = false) String search // name/email/phone
	) {
		String username = jwt.extractUsername(auth.substring(7));
		MlcListFilterDto filter = new MlcListFilterDto(districtId, status, search);
		return ResponseEntity.ok(service.list(username, filter));
	}

	// Get one
	@GetMapping("/{id}")
	public ResponseEntity<MlcInfoDto> getOne(@RequestHeader("Authorization") String auth, @PathVariable Long id) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.getOne(username, id));
	}

	// Update editable fields (name, contact, term, mappings, etc.)
	@PutMapping("/{id}")
	public ResponseEntity<MlcInfoDto> update(@RequestHeader("Authorization") String auth, @PathVariable Long id,
			@RequestBody MlcUpdateDto body) {
		String username = jwt.extractUsername(auth.substring(7));
		return ResponseEntity.ok(service.update(username, id, body));
	}

	// Activate (requires letter: either uploaded file or URL in payload)
	@PostMapping(value = "/{id}/activate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> activate(@RequestHeader("Authorization") String auth, @PathVariable Long id,
			@RequestPart(name = "payload", required = false) MlcStatusUpdateDto payload,
			@RequestPart(name = "letter", required = false) MultipartFile letter) {
		String username = jwt.extractUsername(auth.substring(7));
		String msg = service.setStatus(username, id, Statuses.ACTIVE, payload, letter);
		return ResponseEntity.ok(msg);
	}

	// Deactivate (requires letter: either uploaded file or URL in payload)
	@PostMapping(value = "/{id}/deactivate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> deactivate(@RequestHeader("Authorization") String auth, @PathVariable Long id,
			@RequestPart(name = "payload", required = false) MlcStatusUpdateDto payload,
			@RequestPart(name = "letter", required = false) MultipartFile letter) {
		String username = jwt.extractUsername(auth.substring(7));
		String msg = service.setStatus(username, id, Statuses.INACTIVE, payload, letter);
		return ResponseEntity.ok(msg);
	}
}
