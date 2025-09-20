// src/main/java/com/lsit/dfds/controller/VoucherQueryController.java
package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.VoucherListItemDto;
import com.lsit.dfds.dto.VoucherQueryDto;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.service.VoucherQueryService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/vouchers")
@RequiredArgsConstructor
public class VoucherQueryController {

	private final VoucherQueryService service;
	private final JwtUtil jwtUtil;

	@GetMapping("/search")
	public ResponseEntity<List<VoucherListItemDto>> search(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) DemandStatuses status, @RequestParam(required = false) String q) {
		String username = jwtUtil.extractUsername(auth.substring(7));
		VoucherQueryDto dto = new VoucherQueryDto();
		dto.setFinancialYearId(financialYearId);
		dto.setDistrictId(districtId);
		dto.setStatus(status);
		dto.setQ(q);
		return ResponseEntity.ok(service.listForUser(username, dto));
	}
}
