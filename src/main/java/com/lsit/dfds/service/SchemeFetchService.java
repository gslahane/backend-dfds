// src/main/java/com/lsit/dfds/service/SchemeFetchService.java
package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.SchemeBudgetResponseDto;

public interface SchemeFetchService {

	List<SchemeBudgetResponseDto> fetchSchemesWithFiltersForUser(String username, Long schemeTypeId, String planType,
			Long districtId, Long financialYearId);

	List<SchemeBudgetResponseDto> fetchSchemesWithFilters(Long schemeTypeId, String planType, Long districtId,
			Long financialYearId);
}
