package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.SchemeTypeDropdownDto;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.entity.Work;

public interface DropdownService {

	List<String> getSchemeNamesForUser(String username);

	List<IdNameDto> getAllFinancialYears();

	List<IdNameDto> getAllDivisions();

	List<IdNameDto> getAllDistricts();

	List<IdNameDto> getAllSectors();

	List<IdNameDto> getSchemesBySectorId(Long sectorId);

	List<IdNameDto> getDistrictsByDivisionId(Long divisionId);

	List<IdNameDto> getTalukasByDistrictId(Long districtId);

	List<IdNameDto> getConstituenciesByDistrictId(Long districtId);

	List<SchemeTypeDropdownDto> getAllSchemeTypesWithDetails();

//	List<District> getDistrictsByDivisionId(Long divisionId);
//
//	List<Taluka> getTalukasByDistrictId(Long districtId);
//
//	List<Constituency> getConstituenciesByDistrictId(Long districtId);

	List<Schemes> searchSchemesByFilters(String username, String districtName, Long districtId, String schemeType);

	List<Work> getWorksBySchemeYearAndDistrict(String username, Long schemeId, String financialYear, Long districtId);

}
