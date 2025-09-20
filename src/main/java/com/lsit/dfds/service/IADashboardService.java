package com.lsit.dfds.service;

import java.util.Map;

public interface IADashboardService {
	Map<String, Object> getDashboardData(String username, String financialYear, String planType, Long schemeId,
			Long workId, String status);
}
