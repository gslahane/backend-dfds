package com.lsit.dfds.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.dto.MlaTermUpsertDto;
import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.entity.MLATerm;
import com.lsit.dfds.repo.FinancialYearRepository;
import com.lsit.dfds.repo.MLARepository;
import com.lsit.dfds.repo.MLATermRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MlaTermService {

	private final MLATermRepository termRepo;
	private final MLARepository mlaRepo;
	private final FinancialYearRepository fyRepo;
	private final FinancialYearParser fyParser;

	@Transactional
	public MLATerm upsertTerm(MlaTermUpsertDto dto) {
		MLA mla = mlaRepo.findById(dto.getMlaId()).orElseThrow(() -> new RuntimeException("MLA not found"));

		// Deactivate any existing active term if this is the new one
		termRepo.findFirstByMla_IdAndActiveTrue(mla.getId()).ifPresent(t -> {
			if (Boolean.TRUE.equals(dto.getActive())) {
				t.setActive(false);
				termRepo.save(t);
			}
		});

		MLATerm term = new MLATerm();
		term.setMla(mla);
		term.setActive(dto.getActive() != null ? dto.getActive() : true);
		term.setTenureStartDate(dto.getTenureStartDate());
		term.setTenurePeriod(dto.getTenurePeriod());

		// Calculate tenureEndDate based on tenureStartDate and tenurePeriod
		if (dto.getTenureStartDate() != null && dto.getTenurePeriod() != null) {
			// Example: tenurePeriod = "5 years" or "3 years" (parse number)
			String periodStr = dto.getTenurePeriod().replaceAll("[^0-9]", "");
			int years = 0;
			try {
				years = Integer.parseInt(periodStr);
			} catch (NumberFormatException e) {
				years = 0;
			}
			if (years > 0) {
				term.setTenureEndDate(dto.getTenureStartDate().plusYears(years));
			} else {
				term.setTenureEndDate(dto.getTenureEndDate()); // fallback to provided end date
			}
		} else {
			term.setTenureEndDate(dto.getTenureEndDate());
		}

		term.setTermNumber(dto.getTermNumber());

		return termRepo.save(term);
	}

	public MLATerm getActiveTerm(Long mlaId) {
		return termRepo.findFirstByMla_IdAndActiveTrue(mlaId).orElse(null);
	}
}