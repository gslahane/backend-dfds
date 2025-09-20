package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.DistrictSchemeMappingDto;
import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.SchemeBudget;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.DistrictRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.SchemeBudgetRepository;
import com.lsit.dfds.repo.SchemesRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DistrictSchemeServiceImpl implements DistrictSchemeService {

	private final DistrictRepository districtRepository;
	private final SchemesRepository schemesRepository;

	private final SchemeBudgetRepository schemeBudgetRepository;

	private final FinancialYearRepository financialYearRepository;

	private final DemandCodeRepository demandCodeRepository;

	@Override
	@Transactional
	public String mapSchemesToDistrict(DistrictSchemeMappingDto dto, String createdBy) {

		// 1) Load typed refs (no Objects)
		District district = districtRepository.findById(dto.getDistrictId())
				.orElseThrow(() -> new RuntimeException("District not found"));

		FinancialYear financialYear = financialYearRepository.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));

		DemandCode demandCode = demandCodeRepository.findById(dto.getSchemeTypeId())
				.orElseThrow(() -> new RuntimeException("Demand Code (scheme type) not found"));

		// FY hygiene: demand code must belong to selected FY
		if (!demandCode.getFinancialYear().getId().equals(financialYear.getId())) {
			throw new RuntimeException("Selected Demand Code does not belong to the selected Financial Year");
		}

		// 2) Load schemes and validate
		List<Schemes> schemes = schemesRepository.findAllById(dto.getSchemeIds());
		if (schemes.isEmpty()) {
			throw new RuntimeException("No schemes found for provided IDs");
		}

		for (Schemes s : schemes) {
			// Ensure scheme is in same FY
			if (s.getFinancialYear() == null || !s.getFinancialYear().getId().equals(financialYear.getId())) {
				throw new RuntimeException("Scheme " + s.getId() + " is not in the selected Financial Year");
			}
			// Ensure scheme belongs to this DemandCode (e.g., O27)
			if (s.getSchemeType() == null || !s.getSchemeType().getId().equals(demandCode.getId())) {
				throw new RuntimeException("Scheme " + s.getId() + " does not belong to the selected Demand Code");
			}
			// If these are DAP-only mappings:
			// if (s.getPlanType() != PlanType.DAP) { throw new RuntimeException("Scheme " +
			// s.getId() + " is not DAP"); }
		}

		// 3) Ensure DemandCode <-> District link exists (fills district_scheme_types)
		List<District> dcDistricts = demandCode.getDistricts();
		if (dcDistricts == null) {
			dcDistricts = new java.util.ArrayList<>();
			demandCode.setDistricts(dcDistricts);
		}
		boolean linked = dcDistricts.stream().anyMatch(d -> d.getId().equals(district.getId()));
		if (!linked) {
			dcDistricts.add(district);
			demandCodeRepository.save(demandCode);
		}

		// 4) Ensure SchemeBudget rows for each (district, scheme, FY) with zero
		// allocation
		int created = 0;
		for (Schemes scheme : schemes) {
			boolean exists = schemeBudgetRepository.findByDistrict_IdAndScheme_IdAndFinancialYear_Id(district.getId(),
					scheme.getId(), financialYear.getId()).isPresent();

			if (!exists) {
				SchemeBudget b = new SchemeBudget();
				b.setDistrict(district);
				b.setScheme(scheme);
				b.setFinancialYear(financialYear); // <-- typed FinancialYear, not Object
				b.setAllocatedLimit(0.0);
				b.setUtilizedLimit(0.0);
				b.setRemainingLimit(0.0);
				b.setStatus(Statuses.ACTIVE);
				b.setRemarks(dto.getRemarks()); // <-- DTO now has remarks
				b.setCreatedBy(createdBy);
				schemeBudgetRepository.save(b);
				created++;
			}
		}

		// IMPORTANT: do NOT modify district.getSchemes() here; we’re mapping via
		// DemandCode↔District
		return "Mapped " + schemes.size() + " scheme(s) under Demand Code '" + demandCode.getSchemeType()
				+ "' for district '" + district.getDistrictName() + "' (created " + created + " SchemeBudget rows).";
	}

}
