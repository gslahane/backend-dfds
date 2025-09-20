package com.lsit.dfds.service;

import com.lsit.dfds.dto.DistrictSchemeBudgetDto;

public interface DistrictSchemeBudgetService {
	String allocateBudgetToScheme(DistrictSchemeBudgetDto dto, String createdBy);
}
