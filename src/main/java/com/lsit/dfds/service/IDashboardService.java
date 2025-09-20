package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.AdminDAPBudgetRespDto;
import com.lsit.dfds.dto.AdminDAPDashboardRequest;
import com.lsit.dfds.dto.AdminDAPDashboardResponce;
import com.lsit.dfds.dto.AdminDAPSchemesDto;
import com.lsit.dfds.dto.AdminMLDashboardDto;
import com.lsit.dfds.dto.AdminMLrequestDto;
import com.lsit.dfds.dto.OverallBudgetDto;

public interface IDashboardService {

	List<OverallBudgetDto> getOverallStateBudget(String username, Long financialYear_Id);

	List<AdminDAPDashboardResponce> getAdminDAPDashboard(String username, AdminDAPDashboardRequest request);

	AdminDAPBudgetRespDto getAdminDAPBudget(String username, Long financialYear_Id);

	List<AdminDAPSchemesDto> getAdminDAPSchemes(String username, Long district_Id, Long schemeType_Id,
			Long financialYear_Id);

	List<AdminMLDashboardDto> getAdminMLDashboard(String username, AdminMLrequestDto request);

	AdminDAPBudgetRespDto getAdminMLBudget(String username, Long financialYear_Id);

	AdminDAPBudgetRespDto getAdminDistrictMLBudget(String username, Long financialYear_Id);

}
