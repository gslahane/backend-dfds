package com.lsit.dfds.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.lsit.dfds.dto.TaxRequestDto;
import com.lsit.dfds.entity.Tax;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.TaxRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaxServiceImpl implements TaxService {

	private final TaxRepository taxRepo;

	@Override
	public List<Tax> getAllTaxes(String filterType) {
		if (filterType == null || filterType.equalsIgnoreCase("All")) {
			return taxRepo.findAll(); // ✅ Fetch all (for Admin view)
		}
		return taxRepo.findByTaxTypeIgnoreCaseAndStatus(filterType, Statuses.ACTIVE);
	}

	@Override
	public Tax createTax(TaxRequestDto dto, String username) {
		Tax tax = new Tax();
		tax.setCode(dto.getCode());
		tax.setTaxName(dto.getTaxName());
		tax.setTaxType(dto.getTaxType());
		tax.setTaxPercentage(dto.getTaxPercentage());
		tax.setFixedAmount(dto.getFixedAmount());
		tax.setRemarks(dto.getRemarks());
		tax.setStatus(Statuses.ACTIVE);
		tax.setCreatedBy(username);
		return taxRepo.save(tax);
	}

	@Override
	public void deleteTax(Long taxId) {
		taxRepo.deleteById(taxId); // ✅ Physical delete
	}

	@Override
	public Tax updateStatus(Long taxId, String status, String username) {
		Tax tax = taxRepo.findById(taxId).orElseThrow(() -> new RuntimeException("Tax not found"));
		if (!status.equalsIgnoreCase("ACTIVE") && !status.equalsIgnoreCase("INACTIVE")) {
			throw new RuntimeException("Invalid status value");
		}
		tax.setStatus(Statuses.valueOf(status.toUpperCase()));
		tax.setRemarks("Status changed by " + username);
		return taxRepo.save(tax);
	}
}
