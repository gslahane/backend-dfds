package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.FundDemandFilterRequest;
import com.lsit.dfds.dto.FundDemandResponseDto;

import jakarta.servlet.http.HttpServletRequest;

public interface FundDemandFetchService {
	List<FundDemandResponseDto> getFilteredFundDemands(FundDemandFilterRequest filters, HttpServletRequest request);
}
