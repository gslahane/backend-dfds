package com.lsit.dfds.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsit.dfds.dto.GenericResponseDto;
import com.lsit.dfds.dto.HADPRecommendWorkReqDto;
import com.lsit.dfds.dto.HadpRecomendWorkResponseDto;
import com.lsit.dfds.dto.HadpWorkDetailsDto;
import com.lsit.dfds.dto.MLARecomendWorkReqDto;
import com.lsit.dfds.dto.MLARecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecommendWorkReqDto;
import com.lsit.dfds.dto.WorkAaApprovalDto;
import com.lsit.dfds.dto.WorkApprovalDto;
import com.lsit.dfds.dto.WorkByResponseDto;
import com.lsit.dfds.dto.WorkDetailsWithStatusDto;
import com.lsit.dfds.dto.WorkDto;
import com.lsit.dfds.dto.WorkListFilter;
import com.lsit.dfds.dto.WorkListRowDto;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;
import com.lsit.dfds.service.FileStorageService;
import com.lsit.dfds.service.WorkRecommendationService;
import com.lsit.dfds.service.WorkRecommendationServiceImpl;
import com.lsit.dfds.service.WorkService;
import com.lsit.dfds.utils.JwtUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/works")
@RequiredArgsConstructor
@Tag(name = "Works", description = "Recommend works, multi-stage approvals, and role-aware fetching")
public class WorkRecommendationController {

	private final WorkRecommendationService service;
	private final JwtUtil jwt;
	private final WorkService workService;
	private final FileStorageService fileStorageService;
	private final ObjectMapper objectMapper;

	// ==================== MLA WORK RECOMMENDATION ====================
	@Operation(summary = "Recommend a work (MLA)", description = """
			Submit a new work recommendation by an MLA.
			Multipart parts:
			- `payload` = JSON of MLARecomendWorkReqDto
			- `letter`  = (optional) recommendation letter file
			""", tags = { "Works", "Recommend",
			"MLA" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)), responses = {
					@ApiResponse(responseCode = "200", description = "Work recommended", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
					@ApiResponse(responseCode = "400", description = "Bad request", content = @Content) })
	@PostMapping(value = "/recommend/mla", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<MLARecomendWorkResponseDto>> recommendByMla(
			@RequestHeader("Authorization") String auth, @RequestPart("payload") String payload,
			@RequestPart(name = "letter", required = false) MultipartFile letter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			MLARecomendWorkReqDto dto = objectMapper.readValue(json, MLARecomendWorkReqDto.class);

			String recommendationLetterUrl = null;
			if (letter != null && !letter.isEmpty()) {
				recommendationLetterUrl = fileStorageService.saveFile(letter, "mla-recommendations");
			}

			MLARecomendWorkResponseDto responseDto = service.recommendByMla(dto, username, dto.getWorkCode(),
					recommendationLetterUrl);
			return ResponseEntity
					.ok(new GenericResponseDto<>(true, "Work recommended successfully by MLA", responseDto));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to recommend work: " + e.getMessage(), null));
		}
	}

	// ==================== MLC WORK RECOMMENDATION ====================
	// In your controller
	// ==================== MLC WORK RECOMMENDATION ====================
	@Operation(summary = "Recommend a work (MLC – nodal/non-nodal)", description = """
			Submit a new work recommendation by an MLC, indicating target district (nodal/non-nodal rules apply).
			Multipart parts:
			- `payload` = JSON of MLCRecommendWorkReqDto
			- `letter`  = (optional) recommendation letter file
			""", tags = { "Works", "Recommend",
			"MLC" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)), responses = {
					@ApiResponse(responseCode = "200", description = "Work recommended", content = @Content(schema = @Schema(implementation = GenericResponseDto.class))),
					@ApiResponse(responseCode = "400", description = "Bad request", content = @Content) })
	@PostMapping(value = "/recommend/mlc", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<MLCRecomendWorkResponseDto>> recommendByMlc(
			@RequestHeader("Authorization") String auth, @RequestPart("payload") String payload,
			@RequestPart(name = "letter", required = false) MultipartFile letter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			MLCRecommendWorkReqDto dto = objectMapper.readValue(json, MLCRecommendWorkReqDto.class);

			String recommendationLetterUrl = null;
			if (letter != null && !letter.isEmpty()) {
				recommendationLetterUrl = fileStorageService.saveFile(letter, "mlc-recommendations");
			}

			MLCRecomendWorkResponseDto responseDto = service.recommendByMlc(dto, username, recommendationLetterUrl);
			return ResponseEntity
					.ok(new GenericResponseDto<>(true, "Work recommended successfully by MLC", responseDto));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to recommend work: " + e.getMessage(), null));
		}
	}

	// ==================== HADP WORK RECOMMENDATION ====================
	@Operation(summary = "Recommend a work (HADP by District/HADP Admin)", description = """
			Recommend a HADP work for a specific HADP taluka under the user's district.
			Multipart parts:
			- `payload` = JSON of HADPRecommendWorkReqDto
			- `letter`  = (optional) recommendation letter file
			""", tags = { "Works", "Recommend",
			"HADP" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
    //@PreAuthorize("hasAnyRole('DISTRICT_ADMIN','HADP_ADMIN')")
    @PostMapping(value = "/recommend/hadp",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponseDto<HadpRecomendWorkResponseDto>> recommendByHadp(
            @RequestHeader("Authorization") String auth,
            @RequestPart("payload") String payload,
            @RequestPart(name = "letter", required = false) MultipartFile letter) throws Exception {

        // 🔎 Debugging block
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("==> Authenticated User: " + authentication.getName());
        System.out.println("==> Authorities: ");
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            System.out.println("   - " + authority.getAuthority());
        }

        try {
            String username = jwt.extractUsername(auth.substring(7));
            String json = cleanMultipartJson(payload);
            HADPRecommendWorkReqDto dto = objectMapper.readValue(json, HADPRecommendWorkReqDto.class);

            HadpRecomendWorkResponseDto saved = service.recommendByHadp(dto, username, letter);
            return ResponseEntity.ok(new GenericResponseDto<>(true, "Work recommended successfully under HADP", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new GenericResponseDto<>(false, "Failed to recommend HADP work: " + e.getMessage(), null));
        }
    }

//    @PreAuthorize("hasAnyRole('DISTRICT_ADMIN','HADP_ADMIN')")
//    @PostMapping(value = "/recommend/hadp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<GenericResponseDto<HadpRecomendWorkResponseDto>> recommendByHadp(
//			@RequestHeader("Authorization") String auth, @RequestPart("payload") String payload,
//			@RequestPart(name = "letter", required = false) MultipartFile letter) throws Exception {
//
//        // 🔎 Debugging block
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        System.out.println("==> Authenticated User: " + authentication.getName());
//        System.out.println("==> Authorities: ");
//        for (GrantedAuthority authority : authentication.getAuthorities()) {
//            System.out.println("   - " + authority.getAuthority());
//        }
//
//		try {
//			String username = jwt.extractUsername(auth.substring(7));
//			String json = cleanMultipartJson(payload);
//			HADPRecommendWorkReqDto dto = objectMapper.readValue(json, HADPRecommendWorkReqDto.class);
//
//			HadpRecomendWorkResponseDto saved = service.recommendByHadp(dto, username, letter);
//			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work recommended successfully under HADP", saved));
//		} catch (Exception e) {
//			return ResponseEntity.badRequest()
//					.body(new GenericResponseDto<>(false, "Failed to recommend HADP work: " + e.getMessage(), null));
//		}
//	}

	// ==================== APPROVAL WORKFLOW (ADPO → DPO → COLLECTOR)
	// ====================
	@Operation(summary = "ADPO decision", description = """
			Approve/Reject at ADPO stage. Optionally adjust amount (gross) and upload letter.
			Multipart parts:
			- `payload` = JSON of WorkApprovalDto
			- `approvalLetter` = (optional) file
			""", tags = { "Works", "Approval",
			"District" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
	@PutMapping(value = "/approve/adpo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<WorkDto>> approveByAdpo(@RequestHeader("Authorization") String auth,
			@RequestPart("payload") String payload,
			@RequestPart(name = "approvalLetter", required = false) MultipartFile approvalLetter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			WorkApprovalDto dto = objectMapper.readValue(json, WorkApprovalDto.class);

			String approvalLetterUrl = null;
			if (approvalLetter != null && !approvalLetter.isEmpty()) {
				approvalLetterUrl = fileStorageService.saveFile(approvalLetter, "adpo-approvals");
			}

			Work updated = service.approveByAdpo(dto, username, approvalLetterUrl);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work approved by ADPO", toWorkDto(updated)));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to approve work: " + e.getMessage(), null));
		}
	}

	@Operation(summary = "DPO decision", description = """
			Approve/Reject at DPO stage. Optionally adjust amount (gross) and upload letter.
			Multipart parts:
			- `payload` = JSON of WorkApprovalDto
			- `approvalLetter` = (optional) file
			""", tags = { "Works", "Approval",
			"District" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
	@PutMapping(value = "/approve/dpo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<WorkDto>> approveByDpo(@RequestHeader("Authorization") String auth,
			@RequestPart("payload") String payload,
			@RequestPart(name = "approvalLetter", required = false) MultipartFile approvalLetter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			WorkApprovalDto dto = objectMapper.readValue(json, WorkApprovalDto.class);

			String approvalLetterUrl = null;
			if (approvalLetter != null && !approvalLetter.isEmpty()) {
				approvalLetterUrl = fileStorageService.saveFile(approvalLetter, "dpo-approvals");
			}

			Work updated = service.approveByDpo(dto, username, approvalLetterUrl);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work approved by DPO", toWorkDto(updated)));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to approve work: " + e.getMessage(), null));
		}
	}

	@Operation(summary = "Collector decision", description = """
			Approve/Reject at Collector stage. May assign IA here.
			Multipart parts:
			- `payload` = JSON of WorkApprovalDto
			- `approvalLetter` = (optional) file
			""", tags = { "Works", "Approval",
			"District" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
	@PutMapping(value = "/approve/collector", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<WorkDto>> approveByCollector(@RequestHeader("Authorization") String auth,
			@RequestPart("payload") String payload,
			@RequestPart(name = "approvalLetter", required = false) MultipartFile approvalLetter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			WorkApprovalDto dto = objectMapper.readValue(json, WorkApprovalDto.class);

			String approvalLetterUrl = null;
			if (approvalLetter != null && !approvalLetter.isEmpty()) {
				approvalLetterUrl = fileStorageService.saveFile(approvalLetter, "collector-approvals");
			}

			Work updated = service.approveByCollector(dto, username, approvalLetterUrl);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work approved by Collector", toWorkDto(updated)));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to approve work: " + e.getMessage(), null));
		}
	}

	@Operation(summary = "Reject work (district roles)", description = """
			Reject a work at any district step (ADPO/DPO/Collector).
			Multipart parts:
			- `payload` = JSON of WorkApprovalDto (with remarks)
			- `rejectionLetter` = (optional) file
			""", tags = { "Works", "Approval",
			"District" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
	@PutMapping(value = "/reject", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<WorkDto>> rejectWork(@RequestHeader("Authorization") String auth,
			@RequestPart("payload") String payload,
			@RequestPart(name = "rejectionLetter", required = false) MultipartFile rejectionLetter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			WorkApprovalDto dto = objectMapper.readValue(json, WorkApprovalDto.class);

			String rejectionLetterUrl = null;
			if (rejectionLetter != null && !rejectionLetter.isEmpty()) {
				rejectionLetterUrl = fileStorageService.saveFile(rejectionLetter, "rejections");
			}

			Work updated = service.rejectWork(dto, username, rejectionLetterUrl);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Work rejected", toWorkDto(updated)));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to reject work: " + e.getMessage(), null));
		}
	}

	// ==================== AA APPROVAL (FINAL – budgets adjust here only)
	// ====================
	@Operation(summary = "Admin Approval (AA) & Assign IA/Vendor (budget impact)", description = """
			Finalizes AA amount, sets sanction date and (optionally) assigns IA & vendor.
			Budgets are adjusted **only** at this stage.
			Multipart parts:
			- `payload` = JSON of WorkAaApprovalDto
			- `aaLetter` = (optional) file
			""", tags = { "Works", "Approval",
			"AA" }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)))
	@PutMapping(value = "/aa-approve", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenericResponseDto<WorkDto>> approveAa(@RequestHeader("Authorization") String auth,
			@RequestPart("payload") String payload,
			@RequestPart(name = "aaLetter", required = false) MultipartFile aaLetter) throws Exception {

		try {
			String username = jwt.extractUsername(auth.substring(7));
			String json = cleanMultipartJson(payload);
			WorkAaApprovalDto dto = objectMapper.readValue(json, WorkAaApprovalDto.class);

			Work saved = service.approveAaAndAssign(dto, username, aaLetter);
			return ResponseEntity
					.ok(new GenericResponseDto<>(true, "AA Approval completed successfully", toWorkDto(saved)));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to approve AA: " + e.getMessage(), null));
		}
	}

	// ==================== ROLE-AWARE BROWSE/SEARCH ====================
	@Operation(summary = "Browse/search works (role-aware)", description = """
			Role-aware listing with filters.
			* **STATE**: filter by planType/status/district/MLA/MLC/IA/vendor, etc.
			* **DISTRICT**: scoped to your district (includes IA/MLA/MLC works)
			* **IA**: only your works; **VENDOR**: only assigned/sanctioned/proposed for vendor
			Pagination & sorting supported.
			""", tags = { "Works", "Search", "Browse" })
	@GetMapping("/browse")
	public ResponseEntity<GenericResponseDto<List<WorkDetailsWithStatusDto>>> browse(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) String planType,
			@RequestParam(required = false) String status, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long iaUserId, @RequestParam(required = false) Long vendorId,
			@RequestParam(required = false) String mlaName, @RequestParam(required = false) String mlcName,
			@RequestParam(required = false, name = "q") String query,
			@RequestParam(required = false, defaultValue = "0") Integer page,
			@RequestParam(required = false, defaultValue = "20") Integer size,
			@RequestParam(required = false, defaultValue = "createdAt") String sortBy,
			@RequestParam(required = false, defaultValue = "desc") String sortDir) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			PlanType pt = null;
			if (planType != null && !planType.isBlank()) {
				try {
					pt = PlanType.valueOf(planType.toUpperCase());
				} catch (IllegalArgumentException ignored) {
				}
			}
			WorkStatuses st = null;
			if (status != null && !status.isBlank()) {
				try {
					st = WorkStatuses.valueOf(status.toUpperCase());
				} catch (IllegalArgumentException ignored) {
				}
			}

			List<WorkDetailsWithStatusDto> rows = ((WorkRecommendationServiceImpl) service).browseWorks(username, pt,
					st, districtId, iaUserId, vendorId, mlaName, mlcName, query, page, size, sortBy, sortDir);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched", rows));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to fetch works: " + e.getMessage(), null));
		}
	}

	// ==================== EXISTING FETCH APIs (kept as-is, documented)
	// ====================
	@Operation(summary = "List works by District ID", tags = { "Works", "Fetch" })
	@GetMapping("/by-district/{districtId}")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getByDistrict(
			@RequestHeader("Authorization") String auth,
			@Parameter(description = "District DB id") @PathVariable Long districtId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getByDistrictId(districtId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched by District ID", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "List works by MLA ID", tags = { "Works", "Fetch", "MLA" })
	@GetMapping("/by-mla/{mlaId}")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getByMla(
			@RequestHeader("Authorization") String auth,
			@Parameter(description = "MLA DB id") @PathVariable Long mlaId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getByMlaId(mlaId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched by MLA ID", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "List works by MLC ID", tags = { "Works", "Fetch", "MLC" })
	@GetMapping("/by-mlc/{mlcId}")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getByMlc(
			@RequestHeader("Authorization") String auth,
			@Parameter(description = "MLC DB id") @PathVariable Long mlcId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getByMlcId(mlcId, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched by MLC ID", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "List works that have approved demands", tags = { "Works", "Fetch", "Demands" })
	@GetMapping("/approved-demands")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getApprovedDemands(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long mlaId, @RequestParam(required = false) Long mlcId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getApprovedDemands(districtId, mlaId, mlcId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Approved demands fetched successfully", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "All works (with optional filters)", tags = { "Works", "Fetch" })
	@GetMapping("/all")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getAllWorks(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long mlaId, @RequestParam(required = false) Long mlcId,
			@RequestParam(required = false) String status) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getAllWorksWithFilters(districtId, mlaId, mlcId, status);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "All works fetched successfully", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "MLA dashboard", tags = { "Works", "Dashboard", "MLA" })
	@GetMapping("/mla-dashboard/{mlaId}")
	public ResponseEntity<GenericResponseDto<Object>> getMlaDashboard(@RequestHeader("Authorization") String auth,
			@Parameter(description = "MLA DB id") @PathVariable Long mlaId,
			@RequestParam(required = false) Long financialYearId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			Object result = workService.getMlaDashboard(mlaId, financialYearId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "MLA Dashboard data fetched successfully", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "MLC dashboard", tags = { "Works", "Dashboard", "MLC" })
	@GetMapping("/mlc-dashboard/{mlcId}")
	public ResponseEntity<GenericResponseDto<Object>> getMlcDashboard(@RequestHeader("Authorization") String auth,
			@Parameter(description = "MLC DB id") @PathVariable Long mlcId,
			@RequestParam(required = false) Long financialYearId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			Object result = workService.getMlcDashboard(mlcId, financialYearId, username);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "MLC Dashboard data fetched successfully", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "State dashboard", tags = { "Works", "Dashboard", "STATE" })
	@GetMapping("/state-dashboard")
	public ResponseEntity<GenericResponseDto<Object>> getStateDashboard(@RequestHeader("Authorization") String auth,
			@RequestParam(required = false) Long financialYearId, @RequestParam(required = false) String planType) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			Object result = workService.getStateDashboard(financialYearId, planType);
			return ResponseEntity
					.ok(new GenericResponseDto<>(true, "State Dashboard data fetched successfully", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "List works by status (role-aware)", description = "Filter by status and optionally district/MLA/MLC. Scope is role-aware.", tags = {
			"Works", "Fetch", "Status" })
	@GetMapping("/by-status")
	public ResponseEntity<GenericResponseDto<List<WorkDetailsWithStatusDto>>> getWorksByStatus(
			@RequestHeader("Authorization") String auth, @RequestParam(required = false) String status,
			@RequestParam(required = false) Long districtId, @RequestParam(required = false) Long mlaId,
			@RequestParam(required = false) Long mlcId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			WorkStatuses workStatus = null;
			if (status != null && !status.isEmpty()) {
				try {
					workStatus = WorkStatuses.valueOf(status.toUpperCase());
				} catch (IllegalArgumentException e) {
					return ResponseEntity.badRequest()
							.body(new GenericResponseDto<>(false, "Invalid work status: " + status, null));
				}
			}

			List<WorkDetailsWithStatusDto> works = service.getWorksByStatus(username, workStatus, districtId, mlaId,
					mlcId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works retrieved successfully", works));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new GenericResponseDto<>(false, "Failed to retrieve works: " + e.getMessage(), null));
		}
	}

	// src/main/java/com/lsit/dfds/controller/WorkController.java
	@Operation(summary = "Role-aware list (legacy)", tags = { "Works", "Fetch" })
	@GetMapping("/workList")
	public ResponseEntity<List<WorkListRowDto>> listWorks(@RequestHeader("Authorization") String auth,
			@Parameter(description = "Optional filter object bound from query") WorkListFilter filter) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			return ResponseEntity.ok(workService.listWorksForUser(username, filter));
		} catch (RuntimeException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(List.of());
		}
	}

	@Operation(summary = "List works by Scheme ID", tags = { "Works", "Fetch", "Scheme" })
	@GetMapping("/by-scheme/{schemeId}")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getByScheme(
			@RequestHeader("Authorization") String auth,
			@Parameter(description = "Scheme DB id") @PathVariable Long schemeId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getBySchemeId(schemeId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched by Scheme ID", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@Operation(summary = "List works by Implementing Agency (IA user id)", tags = { "Works", "Fetch", "IA" })
	@GetMapping("/by-ia/{iaUserId}")
	public ResponseEntity<GenericResponseDto<List<WorkByResponseDto>>> getByIa(
			@RequestHeader("Authorization") String auth,
			@Parameter(description = "IA User DB id") @PathVariable Long iaUserId) {
		try {
			String username = jwt.extractUsername(auth.substring(7));
			List<WorkByResponseDto> result = workService.getByIaId(iaUserId);
			return ResponseEntity.ok(new GenericResponseDto<>(true, "Works fetched by IA User ID", result));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(new GenericResponseDto<>(false, "Unauthorized or Invalid Token", null));
		}
	}

	@GetMapping("/hadp-fetch")
	public ResponseEntity<?> fetchHadpWorks(@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Long hadpId, @RequestParam(required = false) Long districtId,
			@RequestParam(required = false) Long talukaId, @RequestParam(required = false) Long financialYearId,
			@RequestParam(required = false) WorkStatuses status, @RequestParam(required = false) HadpType hadpType) {

		String username = jwt.extractUsername(authHeader.substring(7));
		List<HadpWorkDetailsDto> works = service.fetchHadpWorks(username, hadpId, districtId, talukaId,
				financialYearId, status, hadpType);

		return ResponseEntity.ok(works);
	}

	// Helpers
	private WorkDto toWorkDto(Work w) {
		WorkDto d = new WorkDto();
		d.setId(w.getId());
		d.setWorkName(w.getWorkName());
		d.setWorkCode(w.getWorkCode());
		d.setDescription(w.getDescription());
		d.setGrossAmount(w.getGrossAmount());
		d.setBalanceAmount(w.getBalanceAmount());
		d.setStatus(w.getStatus());
		d.setWorkStartDate(w.getWorkStartDate());
		d.setWorkEndDate(w.getWorkEndDate());
		d.setRecommendationLetterUrl(w.getRecommendationLetterUrl());

		if (w.getScheme() != null) {
			d.setSchemeName(w.getScheme().getSchemeName());
			if (w.getScheme().getFinancialYear() != null) {
				d.setFinancialYearId(w.getScheme().getFinancialYear().getId());
			}
		}
		if (w.getDistrict() != null) {
			d.setDistrictId(w.getDistrict().getId());
		}
		if (w.getConstituency() != null) {
			d.setConstituencyId(w.getConstituency().getId());
		}
		if (w.getImplementingAgency() != null) {
			d.setImplementingAgencyName(w.getImplementingAgency().getFullname());
		}
		return d;
	}

	private static String cleanMultipartJson(String raw) {
		if (raw == null) {
			return "{}";
		}
		return raw.trim();
	}
}
