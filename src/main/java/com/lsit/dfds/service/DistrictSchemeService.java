package com.lsit.dfds.service;

import com.lsit.dfds.dto.DistrictSchemeMappingDto;

public interface DistrictSchemeService {
	String mapSchemesToDistrict(DistrictSchemeMappingDto dto, String createdBy);
}
