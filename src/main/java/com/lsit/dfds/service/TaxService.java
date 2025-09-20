package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.TaxRequestDto;
import com.lsit.dfds.entity.Tax;

public interface TaxService {
	List<Tax> getAllTaxes(String filterType);

	Tax createTax(TaxRequestDto dto, String username);

	void deleteTax(Long taxId); // ✅ Physical delete

	Tax updateStatus(Long taxId, String status, String username); // ✅ Active/Inactive
}
