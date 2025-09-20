package com.lsit.dfds.service;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.VendorDto;
import com.lsit.dfds.dto.VendorRegistrationResponse;
import com.lsit.dfds.dto.VendorRequestDto;
import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserRepository;
import com.lsit.dfds.repo.VendorRepository;
import com.lsit.dfds.utils.JwtUtil;
import com.lsit.dfds.utils.PanVerificationTokenUtil;
import com.lsit.dfds.utils.VendorKycUtil;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

	private final VendorRepository vendorRepository;
	private final JwtUtil jwtUtil;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder; // <- add

	private static final String BASE_FOLDER = "uploads/vendors/";

	private final PanVerificationTokenUtil panTokenUtil;
	private final VendorKycUtil vendorKycUtil;

	void validateIAAdmin(HttpServletRequest request) {
		String username = jwtUtil.extractUsernameFromRequest(request);
		User u = userRepository.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
		if (u.getRole() != Roles.IA_ADMIN)
			throw new RuntimeException("Unauthorized – Only IA Admin can register vendor");
	}

	@Override
	@Transactional
	public VendorRegistrationResponse registerVendor(VendorRequestDto dto, HttpServletRequest request) {
		validateIAAdmin(request);

		// Enforce “verify first”
		String pan = requireNonNull(dto.getPanNumber(), "PAN is required").trim().toUpperCase();
		//String token = dto.getPanVerificationToken();
//		if (token == null || !panTokenUtil.validateToken(token, pan)) {
//			throw new RuntimeException("Please verify PAN before registering (invalid/expired token)");
//		}

		// Create user
		String iaUsername = jwtUtil.extractUsernameFromRequest(request);
		User ia = userRepository.findByUsername(iaUsername).orElseThrow(() -> new RuntimeException("User not found"));

		String chosenUsername = chooseUsername(dto);
		chosenUsername = ensureUniqueUsername(chosenUsername);

		String rawPassword = (dto.getDesiredPassword() != null && !dto.getDesiredPassword().isBlank())
				? dto.getDesiredPassword().trim()
				: generateStrongPassword();

		User vendorUser = new User();
		vendorUser.setUsername(chosenUsername);
		vendorUser.setFullname(
				dto.getContactPerson() != null && !dto.getContactPerson().isBlank() ? dto.getContactPerson()
						: dto.getName());
		vendorUser.setEmail(dto.getEmail());
		vendorUser.setMobile(parseLongSafe(dto.getPhone()));
		vendorUser.setPassword(passwordEncoder.encode(rawPassword));
		vendorUser.setRole(Roles.VENDOR);
		vendorUser.setStatus(Statuses.ACTIVE);
		vendorUser.setSupervisor(ia);
		vendorUser.setDistrict(ia.getDistrict());
		vendorUser.setCreatedBy(iaUsername);
		userRepository.save(vendorUser);

		// Create vendor
		Vendor vendor = new Vendor();
		vendor.setUser(vendorUser);
		vendor.setCreatedBy(iaUsername);

		vendor.setName(dto.getName());
		vendor.setContactPerson(dto.getContactPerson());
		vendor.setPhone(dto.getPhone());
		vendor.setEmail(dto.getEmail());
		vendor.setAddress(dto.getAddress());
		vendor.setCity(dto.getCity());
		vendor.setState(dto.getState());
		vendor.setPincode(dto.getPincode());
		vendor.setPanNumber(pan);
		vendor.setGstNumber(dto.getGstNumber());
		vendor.setBankAccountNumber(dto.getBankAccountNumber());
		vendor.setBankIfsc(dto.getBankIfsc());
		vendor.setBankName(dto.getBankName());
		vendor.setBankBranch(dto.getBankBranch());
		vendor.setType(dto.getType());
		vendor.setCategory(dto.getCategory());
		vendor.setStatus(dto.getStatus());
		vendor.setPaymentTerms(dto.getPaymentTerms());
		vendor.setRegistrationDate(dto.getRegistrationDate());

		// Mark PAN verified — we trust the token (no re-call to API here)
		vendor.setPanVerified(true);

		// (Optional) If you must enforce GST verification too, add similar token or
		// call here.

		vendor = vendorRepository.save(vendor);

		return VendorRegistrationResponse.builder().vendorId(vendor.getId()).vendorName(vendor.getName())
				.username(chosenUsername).tempPassword(rawPassword).message("Vendor registered successfully").build();
	}

	// For updates:
	public Vendor updateVendor(Long id, VendorRequestDto dto, HttpServletRequest request) {
		validateIAAdmin(request);
		Vendor existing = vendorRepository.findById(id).orElseThrow(() -> new RuntimeException("Vendor not found"));

		Vendor updating = existing; // map fields…
		updating.setName(dto.getName());
		updating.setContactPerson(dto.getContactPerson());
		updating.setPhone(dto.getPhone());
		updating.setEmail(dto.getEmail());
		updating.setAddress(dto.getAddress());
		updating.setCity(dto.getCity());
		updating.setState(dto.getState());
		updating.setPincode(dto.getPincode());
		updating.setGstNumber(dto.getGstNumber());
		updating.setBankAccountNumber(dto.getBankAccountNumber());
		updating.setBankIfsc(dto.getBankIfsc());
		updating.setBankName(dto.getBankName());
		updating.setBankBranch(dto.getBankBranch());
		updating.setType(dto.getType());
		updating.setCategory(dto.getCategory());
		updating.setStatus(dto.getStatus());
		updating.setPaymentTerms(dto.getPaymentTerms());
		updating.setRegistrationDate(dto.getRegistrationDate());

		// If PAN changed, re-verify now (server-side call)
		String newPan = dto.getPanNumber();
		if (newPan != null)
			updating.setPanNumber(newPan.trim().toUpperCase());
		vendorKycUtil.verifyOnUpdateIfPanChanged(existing, updating);

		return vendorRepository.save(updating);
	}

	@Override
	public void deleteVendor(Long id, HttpServletRequest request) {
		validateIAAdmin(request);
		vendorRepository.deleteById(id);
	}

	@Override
	@Transactional
	public Vendor activateVendor(Long id, HttpServletRequest request) {
		validateIAAdmin(request);
		Vendor v = vendorRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		v.setStatus("Active");
		if (v.getUser() != null) {
			v.getUser().setStatus(com.lsit.dfds.enums.Statuses.ACTIVE);
			userRepository.save(v.getUser());
		}
		return vendorRepository.save(v);
	}

	@Override
	@Transactional
	public Vendor deactivateVendor(Long id, HttpServletRequest request) {
		validateIAAdmin(request);
		Vendor v = vendorRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
		v.setStatus("Inactive");
		if (v.getUser() != null) {
			v.getUser().setStatus(com.lsit.dfds.enums.Statuses.INACTIVE);
			userRepository.save(v.getUser());
		}
		return vendorRepository.save(v);
	}

	@Override
	@Transactional(readOnly = true)
	public List<VendorDto> getVendorDetails(Long districtId, Long vendorId, String search) {
		List<Vendor> vendors = vendorRepository.findAll(); // Default fetch of all vendors

		// Apply filters
		if (districtId != null) {
			vendors = vendors.stream()
					.filter(v -> v.getUser() != null && v.getUser().getDistrict() != null
							&& v.getUser().getDistrict().getId().equals(districtId)) // Fixed: Access District via User
					.collect(Collectors.toList());
		}
		if (vendorId != null) {
			vendors = vendors.stream().filter(v -> v.getId().equals(vendorId)).collect(Collectors.toList());
		}
		if (search != null && !search.isEmpty()) {
			vendors = vendors.stream().filter(v -> v.getName().contains(search) || v.getPhone().contains(search))
					.collect(Collectors.toList());
		}

		// Convert vendors to VendorDto
		List<VendorDto> vendorDtos = vendors.stream().map(v -> {
			VendorDto dto = new VendorDto();
			dto.setId(v.getId());
			dto.setName(v.getName());
			dto.setContactPerson(v.getContactPerson());
			dto.setPhone(v.getPhone());
			dto.setEmail(v.getEmail());
			dto.setAddress(v.getAddress());
			dto.setCity(v.getCity());
			dto.setState(v.getState());
			dto.setPincode(v.getPincode());
			dto.setRegistrationDate(v.getRegistrationDate());
			dto.setVerificationStatus(v.getVerificationStatus());
			dto.setAadhaarVerified(v.getAadhaarVerified());
			dto.setGstVerified(v.getGstVerified());
			dto.setType(v.getType());
			dto.setCategory(v.getCategory());
			dto.setStatus(v.getStatus());
			dto.setPerformanceRating(v.getPerformanceRating());
			dto.setCreditLimit(v.getCreditLimit());
			dto.setPaymentTerms(v.getPaymentTerms());
			dto.setDocumentUrls(v.getDocumentUrls());
			dto.setPaymentEligible(v.getPaymentEligible());
			dto.setLastUpdated(v.getLastUpdated());
			dto.setUpdatedBy(v.getUpdatedBy());
			dto.setCreatedAt(v.getCreatedAt());
			dto.setCreatedBy(v.getCreatedBy());
			dto.setGstNumber(v.getGstNumber());
			dto.setPanNumber(v.getPanNumber());
			dto.setAadhaarNumber(v.getAadhaarNumber());
			dto.setBankName(v.getBankName());
			dto.setBankAccountNumber(v.getBankAccountNumber());
			dto.setBankIfsc(v.getBankIfsc());
			dto.setBankBranch(v.getBankBranch());

			// Implementing agency details
			if (v.getUser() != null) {
				dto.setUsername(v.getUser().getUsername());
				dto.setMobile(v.getUser().getMobile());
			}

			return dto;
		}).collect(Collectors.toList());

		return vendorDtos;
	}

	private Vendor mapDtoToEntity(VendorRequestDto dto, Vendor v) {
		if (v == null) {
			v = new Vendor();
		}
		v.setName(dto.getName());
		v.setAadhaarNumber(dto.getAadhaarNumber());
		v.setGstNumber(dto.getGstNumber());
		v.setPanNumber(dto.getPanNumber());
		v.setContactPerson(dto.getContactPerson());
		v.setPhone(dto.getPhone());
		v.setEmail(dto.getEmail());
		v.setAddress(dto.getAddress());
		v.setCity(dto.getCity());
		v.setState(dto.getState());
		v.setPincode(dto.getPincode());
		v.setRegistrationDate(dto.getRegistrationDate());
		v.setVerificationStatus(dto.getVerificationStatus());
		v.setAadhaarVerified(dto.getAadhaarVerified());
		v.setGstVerified(dto.getGstVerified());
		v.setType(dto.getType());
		v.setCategory(dto.getCategory());
		v.setStatus(dto.getStatus());
		v.setPerformanceRating(dto.getPerformanceRating());
		v.setCreditLimit(dto.getCreditLimit());
		v.setPaymentTerms(dto.getPaymentTerms());
		v.setBankAccountNumber(dto.getBankAccountNumber());
		v.setBankIfsc(dto.getBankIfsc());
		v.setBankName(dto.getBankName());
		v.setBankBranch(dto.getBankBranch());
		v.setPaymentEligible(dto.getPaymentEligible());
		v.setLastUpdated(LocalDateTime.now().toLocalDate());

		return v;
	}

	private List<String> saveDocuments(List<MultipartFile> files, String vendorName) {
		List<String> urls = new ArrayList<>();
		if (files == null) {
			return urls;
		}

		String folderPath = BASE_FOLDER + vendorName.replaceAll("\\s+", "_") + "/";
		new File(folderPath).mkdirs();

		for (MultipartFile file : files) {
			String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
			try {
				file.transferTo(new File(folderPath + filename));
				urls.add(folderPath + filename);
			} catch (IOException e) {
				throw new RuntimeException("Failed to save file: " + filename);
			}
		}
		return urls;
	}

	@Override
	public List<Vendor> getAllVendors() {
		// TODO Auto-generated method stub
		return null;
	}

	private String chooseUsername(VendorRequestDto dto) {
		if (dto.getDesiredUsername() != null && !dto.getDesiredUsername().isBlank())
			return dto.getDesiredUsername().trim();
		if (dto.getEmail() != null && !dto.getEmail().isBlank())
			return dto.getEmail().trim().toLowerCase();
		if (dto.getPhone() != null && !dto.getPhone().isBlank())
			return dto.getPhone().trim();
		return "vendor" + System.currentTimeMillis(); // fallback
	}

	private String ensureUniqueUsername(String base) {
		String uname = base;
		int i = 1;
		while (userRepository.existsByUsername(uname)) {
			uname = base + i;
			i++;
		}
		return uname;
	}

	private String generateStrongPassword() {
		// simple 12-char random (letters+digits)
		String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
		StringBuilder sb = new StringBuilder();
		java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
		for (int i = 0; i < 12; i++)
			sb.append(chars.charAt(r.nextInt(chars.length())));
		return sb.toString();
	}

	private Long parseLongSafe(String s) {
		try {
			return (s == null || s.isBlank()) ? 0L : Long.parseLong(s.replaceAll("\\D", ""));
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

}
