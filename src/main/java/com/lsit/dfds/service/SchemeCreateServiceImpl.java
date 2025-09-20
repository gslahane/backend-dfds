package com.lsit.dfds.service;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.SchemeCreateDto;
import com.lsit.dfds.dto.SchemeResponseDto;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Sector;
import com.lsit.dfds.repo.DemandCodeRepository;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.SchemesRepository;
import com.lsit.dfds.repo.SectorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchemeCreateServiceImpl implements SchemeCreateService {

	private final SchemesRepository schemesRepository;
	private final FinancialYearRepository financialYearRepository;
	private final DemandCodeRepository schemeTypeRepository;
	private final SectorRepository sectorRepository;

	@Override
	public SchemeResponseDto createScheme(SchemeCreateDto dto, String createdBy) {

		FinancialYear financialYear = financialYearRepository.findById(dto.getFinancialYearId())
				.orElseThrow(() -> new RuntimeException("Financial Year not found"));

		Sector sector = sectorRepository.findById(dto.getSectorId())
				.orElseThrow(() -> new RuntimeException("Sector not found"));

		Schemes scheme = new Schemes();
		scheme.setSchemeName(dto.getSchemeName());
		scheme.setSchemeCode(dto.getSchemeCode());
		scheme.setCrcCode(dto.getCrcCode());
		scheme.setObjectCode(dto.getObjectCode());
		scheme.setObjectName(dto.getObjectName());
		scheme.setMajorHeadName(dto.getMajorHeadName());
		scheme.setDescription(dto.getDescription());

		scheme.setAllocatedBudget(0.0);
		scheme.setEstimatedBudget(0.0);
		scheme.setRevisedBudget(0.0);
		scheme.setNextBudget(0.0);

		scheme.setFinancialYear(financialYear);
		scheme.setBaseSchemeType(dto.getBaseSchemeType());
		scheme.setSector(sector);
		scheme.setPlanType(dto.getPlanType());
		scheme.setCreatedBy(createdBy);

		Schemes saved = schemesRepository.save(scheme);

		return new SchemeResponseDto(saved.getId(), saved.getSchemeName(), saved.getSchemeCode(),
				saved.getDescription(), saved.getBaseSchemeType(), saved.getPlanType(), financialYear.getFinacialYear(), // Assuming
																															// you
																															// have
																															// getYearName()
																															// in
																															// FinancialYear
				sector.getSectorName(), saved.getCreatedBy());
	}

}
