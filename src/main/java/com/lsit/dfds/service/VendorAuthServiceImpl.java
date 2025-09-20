// com.lsit.dfds.service.VendorAuthServiceImpl
package com.lsit.dfds.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.VendorAccountProvisionDto;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorAuthServiceImpl implements VendorAuthService {

	private final VendorRepository vendorRepo;
	private final UserRepository userRepo;
	private final PasswordEncoder passwordEncoder;
	private final AuditService audit;

	private void ensureProvisionRights(User actor) {
		// Allow STATE_*, DISTRICT_* and IA_ADMIN to provision vendor accounts
		Roles r = actor.getRole();
		if (!(r.name().startsWith("STATE") || r.name().startsWith("DISTRICT") || r == Roles.IA_ADMIN)) {
			throw new RuntimeException("Unauthorized: only STATE/DISTRICT/IA can provision vendor accounts");
		}
	}

	@Override
	@Transactional
	public User provisionVendorUser(VendorAccountProvisionDto dto, String createdBy) {
		User actor = userRepo.findByUsername(createdBy).orElseThrow(() -> new RuntimeException("Creator not found"));
		ensureProvisionRights(actor);

		Vendor v = vendorRepo.findById(dto.getVendorId()).orElseThrow(() -> new RuntimeException("Vendor not found"));

		if (v.getUser() != null) {
			throw new RuntimeException("Login already provisioned for this vendor");
		}

		if (dto.getUsername() == null || dto.getUsername().isBlank()) {
			throw new RuntimeException("username is required");
		}
		if (dto.getPassword() == null || dto.getPassword().isBlank()) {
			throw new RuntimeException("password is required");
		}
		if (userRepo.findByUsername(dto.getUsername()).isPresent()) {
			throw new RuntimeException("Username already taken");
		}

		User u = new User();
		u.setUsername(dto.getUsername());
		u.setPassword(passwordEncoder.encode(dto.getPassword()));
		u.setEmail(dto.getEmail());
		u.setMobile(dto.getMobile());
		u.setFullname(dto.getFullName() != null ? dto.getFullName() : v.getName());
		u.setRole(Roles.VENDOR);
		u.setStatus(Statuses.ACTIVE);
		u.setCreatedBy(createdBy);

		// District is optional for vendors; omit or set from actor if you need locality
		// scoping
		// u.setDistrict(actor.getDistrict());

		User saved = userRepo.save(u);

		v.setUser(saved);
		vendorRepo.save(v);

		audit.log(createdBy, "PROVISION_VENDOR_USER", "Vendor", v.getId(),
				"{\"username\":\"" + saved.getUsername() + "\"}");

		return saved;
	}

	@Override
	@Transactional
	public void revokeVendorUser(Long vendorId, String requestedBy) {
		User actor = userRepo.findByUsername(requestedBy)
				.orElseThrow(() -> new RuntimeException("Requester not found"));
		ensureProvisionRights(actor);

		Vendor v = vendorRepo.findById(vendorId).orElseThrow(() -> new RuntimeException("Vendor not found"));
		if (v.getUser() == null) {
			return;
		}

		// You can disable instead of deleting:
		User u = v.getUser();
		u.setStatus(Statuses.INACTIVE);
		userRepo.save(u);

		audit.log(requestedBy, "REVOKE_VENDOR_USER", "Vendor", v.getId(), null);
	}
}
