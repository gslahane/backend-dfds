package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody; // Spring's RequestBody
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lsit.dfds.dto.FundDemandDetailBundleDto;
import com.lsit.dfds.dto.FundDemandFilterRequest;
import com.lsit.dfds.dto.FundDemandListItemDto;
import com.lsit.dfds.dto.FundDemandQueryDto;
import com.lsit.dfds.dto.FundDemandRequestDto;
import com.lsit.dfds.dto.FundDemandResponseDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateResult;
import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.enums.DemandStatuses;
import com.lsit.dfds.service.FundDemandFetchService;
import com.lsit.dfds.service.FundDemandQueryService;
import com.lsit.dfds.service.FundDemandService;
import com.lsit.dfds.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
// If you already have a bearerAuth scheme defined, you can add:
// import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/fund-demands")
@RequiredArgsConstructor
@Tag(name = "Fund Demands", description = "Create, approve, search and manage fund demands under approved works")
// @SecurityRequirement(name = "bearerAuth")
public class FundDemandController {

	private final FundDemandService fundDemandService;
	private final FundDemandQueryService fundDemandQueryService;
	private final FundDemandFetchService fundDemandFetchService;
	private final JwtUtil jwtUtil;

	@Operation(summary = "Create a fund demand (IA only)", description = """
			Raises a fund demand **after AA approval** of the work. Reserves from Work.balance immediately.
			* **Role**: IA Admin only
			* **Preconditions**: Work status IN [AA_APPROVED, WORK_ORDER_ISSUED, IN_PROGRESS] and within 2 years of sanction date
			* **Budget**: On create, Work.balance -= amount; On reject/delete, restored
			""", tags = {
			"Fund Demands", "Create",
			"IA" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Demand payload with gross amount, net payable, and FE-calculated taxes", content = @Content(schema = @Schema(implementation = FundDemandRequestDto.class))), responses = {
					@ApiResponse(responseCode = "200", description = "Created", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
					@ApiResponse(responseCode = "400", description = "Bad request", content = @Content),
					@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
	@PostMapping("/create")
	public ResponseEntity<GenericResponseDto<FundDemandResponseDto>> createFundDemand(
			@RequestBody FundDemandRequestDto dto, HttpServletRequest request) {
		try {
			FundDemand saved = fundDemandService.createFundDemand(dto, request);
			FundDemandResponseDto out = fundDemandService.toResponseDto(saved);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Fund demand created", out));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	@Operation(summary = "List fund demands visible to current user (role-aware)", description = """
			Role-scoped listing:
			* **STATE**: all demands
			* **DISTRICT**: only demands for works in user's district
			* **IA**: only demands created by the current IA user
			* **VENDOR**: only demands where vendor email matches current user
			""", tags = { "Fund Demands", "List", "Role-Aware" }, responses = {
			@ApiResponse(responseCode = "200", description = "Fetched", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))) })
	@GetMapping
	public ResponseEntity<GenericResponseDto<List<FundDemandListItemDto>>> getFundDemands(HttpServletRequest request) {
		var rows = fundDemandService.getFundDemandsForUserAsList(request);
		return ResponseEntity.ok(new GenericResponseDto<>(true, "Demands fetched", rows));
	}

	@Operation(summary = "Search fund demands (role-aware filters)", description = """
			Search with optional filters.
			* **Filters**: financialYearId, districtId (STATE only), status, q (demandId/vendor/work name/code)
			* **Scope**: same as role-aware list
			""", tags = { "Fund Demands", "Search", "STATE", "DISTRICT", "IA",
			"VENDOR" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Search criteria", content = @Content(schema = @Schema(implementation = FundDemandQueryDto.class))), responses = {
					@ApiResponse(responseCode = "200", description = "Fetched", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
					@ApiResponse(responseCode = "400", description = "Bad request", content = @Content) })
	@PostMapping("/search")
	public ResponseEntity<GenericResponseDto<List<FundDemandListItemDto>>> search(@RequestBody FundDemandQueryDto q,
			HttpServletRequest request) {
		try {
			String username = jwtUtil.extractUsernameFromRequest(request);
			var result = fundDemandService.search(q, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Demands fetched", result));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	@Operation(summary = "Get a single fund demand with details", description = """
			Returns full detail including scheme/work/vendor/taxes.
			* **Scope**: role-aware; forbidden if outside caller's scope
			""", tags = { "Fund Demands", "Details" }, responses = {
			@ApiResponse(responseCode = "200", description = "Fetched", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Bad request / Forbidden", content = @Content) })
	@GetMapping("/{id}")
	public ResponseEntity<GenericResponseDto<FundDemandResponseDto>> getOne(
			@Parameter(description = "Fund demand DB id", required = true) @PathVariable Long id,
			HttpServletRequest request) {
		try {
			String username = jwtUtil.extractUsernameFromRequest(request);
			var dto = fundDemandService.getDemand(id, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Demand fetched", dto));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	@Operation(summary = "List demands for a given work", description = """
			Returns all demands raised under a specific work.
			* **Scope**: role-aware; filtered to caller's district/ownership
			""", tags = { "Fund Demands", "List", "Work" }, responses = {
			@ApiResponse(responseCode = "200", description = "Fetched", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Bad request / Forbidden", content = @Content) })
	@GetMapping("/work/{workId}")
	public ResponseEntity<GenericResponseDto<List<FundDemandListItemDto>>> byWork(
			@Parameter(description = "Work DB id", required = true) @PathVariable Long workId,
			HttpServletRequest request) {
		try {
			String username = jwtUtil.extractUsernameFromRequest(request);
			var rows = fundDemandService.listByWork(workId, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work demands fetched", rows));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	@Operation(summary = "Delete a fund demand (IA only, limited statuses)", description = """
			Deletes a demand only when in PENDING or sent-back/rejected states.
			* **Role**: IA Admin only
			* **Budget**: Restores reserved Work.balance
			""", tags = { "Fund Demands", "Delete", "IA" }, responses = {
			@ApiResponse(responseCode = "200", description = "Deleted", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Bad request", content = @Content) })
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<GenericResponseDto<String>> deleteFundDemand(
			@Parameter(description = "Fund demand DB id", required = true) @PathVariable Long id,
			HttpServletRequest request) {
		try {
			String msg = fundDemandService.deleteFundDemand(id, request);
			return ResponseEntity.ok(new GenericResponseDto<>(true, msg, null));
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	@Operation(summary = "Update fund demand status (District/State workflow)", description = """
			Approval workflow endpoint.
			* **District** can set: APPROVED_BY_DISTRICT, REJECTED_BY_DISTRICT, SENT_BACK_BY_DISTRICT
			* **State** can set: APPROVED_BY_STATE, REJECTED_BY_STATE, SENT_BACK_BY_STATE
			* Supports single id, list of ids, or approveAll=TRUE scoped to caller
			* **Budget**: On REJECT, Work.balance is restored and tax record cancelled
			""", tags = { "Fund Demands", "Approval", "DISTRICT",
			"STATE" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Status update request for single/bulk/all", content = @Content(schema = @Schema(implementation = FundDemandStatusUpdateDto.class))), responses = {
					@ApiResponse(responseCode = "200", description = "Status updated", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
					@ApiResponse(responseCode = "400", description = "Bad request / Invalid state transition", content = @Content),
					@ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content) })
	@PutMapping("/update-status")
	public ResponseEntity<GenericResponseDto<FundDemandStatusUpdateResult>> updateFundDemandStatus(
			@RequestBody FundDemandStatusUpdateDto dto, HttpServletRequest request) {

		String username = jwtUtil.extractUsernameFromRequest(request);
		if (username == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized", null));
		}

		try {
			// Returns a batch-safe summary (updated count, ids, errors)
			FundDemandStatusUpdateResult result = fundDemandService.updateFundDemandStatus(dto, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Status updated", result));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

	// Convenience helper (optional)
	public String extractUsernameFromRequest(HttpServletRequest request) {
		final String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			return jwtUtil.extractUsername(authHeader.substring(7));
		}
		return null;
	}

	@PostMapping("/filter")
	public ResponseEntity<?> getFilteredDemands(@RequestBody FundDemandFilterRequest filterRequest,
			HttpServletRequest request) {

		try {
			List<FundDemandResponseDto> response = fundDemandFetchService.getFilteredFundDemands(filterRequest,
					request);
			return ResponseEntity.ok(response);
		} catch (RuntimeException e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping("/query-search")
	public ResponseEntity<List<FundDemandListItemDto>> search(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) DemandStatuses status, @RequestParam(required = false) String q) {
		String username = jwtUtil.extractUsername(auth.substring(7));
		FundDemandQueryDto dto = new FundDemandQueryDto();
		dto.setFinancialYearId(financialYearId);
		dto.setDistrictId(districtId);
		dto.setStatus(status);
		dto.setQ(q);
		return ResponseEntity.ok(fundDemandQueryService.listForUser(username, dto));
	}

	// src/main/java/com/lsit/dfds/controller/FundDemandController.java

	@Operation(summary = "Get a single fund demand with details", description = """
			Returns full detail split into lists: demands[], works[], vendors[], taxes[].
			* Scope: role-aware; forbidden if outside caller's scope
			""", tags = { "Fund Demands", "Details" }, responses = {
			@ApiResponse(responseCode = "200", description = "Fetched", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
			@ApiResponse(responseCode = "400", description = "Bad request / Forbidden", content = @Content) })
	@GetMapping("/detailed-fund-demands/{id}")
	public ResponseEntity<GenericResponseDto<FundDemandDetailBundleDto>> getOneDemand(
			@Parameter(description = "Fund demand DB id", required = true) @PathVariable Long id,
			HttpServletRequest request) {
		try {
			String username = jwtUtil.extractUsernameFromRequest(request);
			var bundle = fundDemandService.getDemandBundle(id, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Demand fetched", bundle));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(new GenericResponseDto<>(false, e.getMessage(), null));
		}
	}

}
