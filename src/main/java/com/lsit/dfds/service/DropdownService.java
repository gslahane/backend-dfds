package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.entity.Constituency;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.Division;
import com.lsit.dfds.entity.Taluka;

public interface DropdownService {

	List<String> getSchemeNamesForUser(String username);

	List<Division> getAllDivisions();

	List<District> getAllDistricts();

	List<District> getDistrictsByDivisionId(Long divisionId);

	List<Taluka> getTalukasByDistrictId(Long districtId);

	List<Constituency> getConstituenciesByDistrictId(Long districtId);

}
