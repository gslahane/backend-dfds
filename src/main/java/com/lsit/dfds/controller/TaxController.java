package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.TaxRequestDto;
import com.lsit.dfds.entity.Tax;
import com.lsit.dfds.service.TaxService;
import com.lsit.dfds.utils.JwtUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tax-master")
@RequiredArgsConstructor
public class TaxController {

	private final TaxService taxService;
	private final JwtUtil jwtUtil;

	@GetMapping
	public ResponseEntity<List<Tax>> getTaxes(@RequestParam(required = false) String filterType) {
		return ResponseEntity.ok(taxService.getAllTaxes(filterType));
	}

	@PostMapping
	public ResponseEntity<Tax> createTax(@RequestHeader("Authorization") String authHeader,
			@RequestBody TaxRequestDto dto) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(taxService.createTax(dto, username));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteTax(@PathVariable Long id) {
		taxService.deleteTax(id);
		return ResponseEntity.ok("Tax deleted successfully");
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Tax> updateStatus(@RequestHeader("Authorization") String authHeader, @PathVariable Long id,
			@RequestParam String status) {
		String username = jwtUtil.extractUsername(authHeader.substring(7));
		return ResponseEntity.ok(taxService.updateStatus(id, status, username));
	}
}
