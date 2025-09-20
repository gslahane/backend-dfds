// src/main/java/com/lsit/dfds/service/MlaAdminServiceImpl.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlaInfoDto;
import com.lsit.dfds.dto.MlaListFilterDto;
import com.lsit.dfds.dto.MlaStatusUpdateDto;
import com.lsit.dfds.dto.MlaUpdateDto;
import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.ConstituencyRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MlaAdminServiceImpl implements MlaAdminService {

	private final UserRepository userRepo;
	private final MLARepository mlaRepo;
	private final ConstituencyRepository constituencyRepo;
	private final SchemesRepository schemesRepo;
	private final FileStorageService fileStorageService;
	private final MLATermRepository mlaTermRepo;

	// ---------- helpers ----------
	private User me(String username) {
		return userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
	}

	private boolean isState(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("STATE");
	}

	private boolean isDistrict(User u) {
		return u.getRole() != null && u.getRole().name().startsWith("DISTRICT");
	}

	private void ensureCanView(User u, MLA m) {
		if (isState(u)) {
			return;
		}
		if (isDistrict(u)) {
			Long ud = u.getDistrict() != null ? u.getDistrict().getId() : null;
			Long md = m.getConstituency() != null && m.getConstituency().getDistrict() != null
					? m.getConstituency().getDistrict().getId()
					: null;
			if (ud == null || md == null || !ud.equals(md)) {
				throw new RuntimeException("Forbidden: MLA belongs to another district");
			}
			return;
		}
		throw new RuntimeException("Forbidden");
	}

	private void ensureCanEdit(User u, MLA m) {
		ensureCanView(u, m);
	}

	private static MlaInfoDto toDto(MLA m, MLATermRepository mlaTermRepo) {
		MlaInfoDto dto = new MlaInfoDto();
		dto.setId(m.getId());
		dto.setMlaName(m.getMlaName());
		dto.setParty(m.getParty());
		dto.setContactNumber(m.getContactNumber());
		dto.setEmail(m.getEmail());
		dto.setStatus(m.getStatus() != null ? m.getStatus().name() : null);

		// Set term info from current active MLATerm
		MLATerm term = mlaTermRepo.findFirstByMla_IdAndActiveTrue(m.getId()).orElse(null);
		if (term != null) {
			dto.setTermNumber(term.getTermNumber());
			dto.setTenureStartDate(term.getTenureStartDate());
			dto.setTenureEndDate(term.getTenureEndDate());
			dto.setTenurePeriod(term.getTenurePeriod());
		}

		if (m.getConstituency() != null) {
			dto.setConstituencyId(m.getConstituency().getId());
			dto.setConstituencyName(m.getConstituency().getConstituencyName());
			if (m.getConstituency().getDistrict() != null) {
				dto.setDistrictId(m.getConstituency().getDistrict().getId());
				dto.setDistrictName(m.getConstituency().getDistrict().getDistrictName());
			}
		}

		// Map defaultScheme -> singleton lists to keep DTO shape
		if (m.getDefaultScheme() != null) {
			Schemes s = m.getDefaultScheme();
			dto.setSchemeIds(s.getId() != null ? List.of(s.getId()) : List.of());
			dto.setSchemeNames(s.getSchemeName() != null ? List.of(s.getSchemeName()) : List.of());
		} else {
			dto.setSchemeIds(List.of());
			dto.setSchemeNames(List.of());
		}

		if (m.getUser() != null) {
			dto.setUserId(m.getUser().getId());
			dto.setUsername(m.getUser().getUsername());
		}
		return dto;
	}

	// ---------- list/search ----------
	@Override
	@Transactional(readOnly = true)
	public List<MlaInfoDto> list(String username, MlaListFilterDto filter) {
		User u = me(username);

		Long districtFilter = filter != null ? filter.getDistrictId() : null;
		Long constituencyFilter = filter != null ? filter.getConstituencyId() : null;
		String statusStr = filter != null ? nz(filter.getStatus()) : null;
		String search = filter != null ? nz(filter.getSearch()) : null;

		// District users default to their district if none specified
		if (isDistrict(u) && districtFilter == null) {
			if (u.getDistrict() == null) {
				throw new RuntimeException("Caller not mapped to a district");
			}
			districtFilter = u.getDistrict().getId();
		}

		// Convert status string -> enum (repo expects Statuses)
		Statuses statusEnum = toStatusesOrNull(statusStr);

		var list = mlaRepo.searchAll(districtFilter, constituencyFilter, statusEnum, search);

		// Enforce visibility just in case
		return list.stream().filter(m -> {
			try {
				ensureCanView(u, m);
				return true;
			} catch (Exception e) {
				return false;
			}
		}).map(m -> toDto(m, mlaTermRepo)).toList();
	}

	private static Statuses toStatusesOrNull(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return Statuses.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(
					"Invalid status: " + status + ". Allowed: " + java.util.Arrays.toString(Statuses.values()));
		}
	}

	// ---------- get one ----------
	@Override
	@Transactional(readOnly = true)
	public MlaInfoDto getOne(String username, Long mlaId) {
		User u = me(username);
		MLA m = mlaRepo.findById(mlaId).orElseThrow(() -> new RuntimeException("MLA not found"));
		ensureCanView(u, m);
		return toDto(m, mlaTermRepo);
	}

	// ---------- update ----------
	@Override
	@Transactional
	public MlaInfoDto update(String username, Long mlaId, MlaUpdateDto dto) {
		User u = me(username);
		MLA m = mlaRepo.findById(mlaId).orElseThrow(() -> new RuntimeException("MLA not found"));
		ensureCanEdit(u, m);

		if (dto.getMlaName() != null) {
			m.setMlaName(dto.getMlaName());
		}
		if (dto.getParty() != null) {
			m.setParty(dto.getParty());
		}
		if (dto.getContactNumber() != null) {
			m.setContactNumber(dto.getContactNumber());
		}
		if (dto.getEmail() != null) {
			m.setEmail(dto.getEmail());
		}

		if (dto.getConstituencyId() != null) {
			Constituency c = constituencyRepo.findById(dto.getConstituencyId())
					.orElseThrow(() -> new RuntimeException("Constituency not found"));
			// District users can only move within their district
			if (isDistrict(u)) {
				Long ud = u.getDistrict() != null ? u.getDistrict().getId() : null;
				Long cd = c.getDistrict() != null ? c.getDistrict().getId() : null;
				if (ud == null || cd == null || !ud.equals(cd)) {
					throw new RuntimeException("Cannot reassign MLA to constituency in another district");
				}
			}
			m.setConstituency(c);
		}

		// ------ DEFAULT SCHEME HANDLING ------
		final Long defaultSchemeIdVal = resolveDefaultSchemeId(dto);
		if (defaultSchemeIdVal != null) {
			Schemes s = schemesRepo.findById(defaultSchemeIdVal)
					.orElseThrow(() -> new RuntimeException("Scheme not found: " + defaultSchemeIdVal));
			m.setDefaultScheme(s);
		}
		// If null, keep existing defaultScheme (field is non-nullable per your entity).

		mlaRepo.save(m);
		return toDto(m, mlaTermRepo);
	}

	// Try dto.getDefaultSchemeId(); else fallback to first of dto.getSchemeIds()
	private Long resolveDefaultSchemeId(MlaUpdateDto dto) {
		if (dto == null) {
			return null;
		}

		try {
			var m = dto.getClass().getMethod("getDefaultSchemeId");
			Object val = m.invoke(dto);
			if (val instanceof Long l) {
				return l;
			}
			if (val instanceof Number n) {
				return n.longValue();
			}
		} catch (NoSuchMethodException ignore) {
			// DTO may not have this; fallback below
		} catch (Exception e) {
			throw new RuntimeException("Failed to read defaultSchemeId from DTO", e);
		}

		if (dto.getSchemeIds() != null && !dto.getSchemeIds().isEmpty()) {
			return dto.getSchemeIds().get(0);
		}
		return null;
	}

	private static Statuses parseStatus(String raw) {
		if (raw == null) {
			throw new RuntimeException("status is required");
		}
		try {
			return Statuses.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException("Invalid status: " + raw);
		}
	}

	// ---------- status (activate/deactivate) ----------
	@Override
	@Transactional
	public MlaInfoDto updateStatus(String username, Long mlaId, MlaStatusUpdateDto dto, MultipartFile letter) {
		User u = me(username);
		MLA m = mlaRepo.findById(mlaId).orElseThrow(() -> new RuntimeException("MLA not found"));
		ensureCanEdit(u, m);

		Statuses st = parseStatus(dto.getStatus());

		// Require a letter either as upload or as URL
		String uploadedUrl = fileStorageService.saveFile(letter, "mla-status-letters");
		String finalUrl = (uploadedUrl != null && !uploadedUrl.isBlank()) ? uploadedUrl
				: nz(dto.getStatusChangeLetterUrl());

		if (finalUrl == null) {
			throw new RuntimeException("Status change letter is required (upload a file or provide a URL).");
		}

		m.setStatus(st);
		m.setStatusChangeLetterUrl(finalUrl);

		if (Boolean.TRUE.equals(dto.getAlsoToggleUser()) && m.getUser() != null) {
			m.getUser().setStatus(st);
			userRepo.save(m.getUser());
		}

		mlaRepo.save(m);
		return toDto(m, mlaTermRepo);
	}

	// Simple overload (no file upload requirement)
	@Override
	@Transactional
	public MlaInfoDto updateStatus(String username, Long mlaId, MlaStatusUpdateDto dto) {
		User u = me(username);
		MLA m = mlaRepo.findById(mlaId).orElseThrow(() -> new RuntimeException("MLA not found"));
		ensureCanEdit(u, m);

		Statuses st = parseStatus(dto.getStatus());
		m.setStatus(st);

		if (Boolean.TRUE.equals(dto.getAlsoToggleUser()) && m.getUser() != null) {
			m.getUser().setStatus(st);
			userRepo.save(m.getUser());
		}

		mlaRepo.save(m);
		return toDto(m, mlaTermRepo);
	}

	private static String nz(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}
}