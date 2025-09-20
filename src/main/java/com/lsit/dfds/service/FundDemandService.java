package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.FundDemandDetailBundleDto;
import com.lsit.dfds.dto.FundDemandListItemDto;
import com.lsit.dfds.dto.FundDemandQueryDto;
import com.lsit.dfds.dto.FundDemandRequestDto;
import com.lsit.dfds.dto.FundDemandResponseDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateDto;
import com.lsit.dfds.dto.FundDemandStatusUpdateResult;
import com.lsit.dfds.dto.FundTransferExecuteDto;
import com.lsit.dfds.dto.TaxTransferExecuteDto;
import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.entity.FundTransfer;
import com.lsit.dfds.entity.TaxDeductionFundTransfer;

import jakarta.servlet.http.HttpServletRequest;

public interface FundDemandService {

	FundDemand createFundDemand(FundDemandRequestDto dto, HttpServletRequest request);

	List<FundDemand> getFundDemandsForUser(HttpServletRequest request);

	// helper for controller to list as DTO
	List<FundDemandListItemDto> getFundDemandsForUserAsList(HttpServletRequest request);

	String deleteFundDemand(Long id, HttpServletRequest request);

	FundDemandStatusUpdateResult updateFundDemandStatus(FundDemandStatusUpdateDto dto, String username);

	// Search / details
	List<FundDemandListItemDto> search(FundDemandQueryDto q, String username);

	FundDemandResponseDto getDemand(Long id, String username);

	List<FundDemandListItemDto> listByWork(Long workId, String username);

	// Mapping helpers
	FundDemandResponseDto toResponseDto(FundDemand d);

	FundDemandListItemDto toListItem(FundDemand d);

	// ✅ add these so controllers don't cast:
	FundTransfer executeVendorTransfer(FundTransferExecuteDto dto, String username);

	TaxDeductionFundTransfer executeTaxTransfer(TaxTransferExecuteDto dto, String username);
	
	   // NEW: bundle response
    FundDemandDetailBundleDto getDemandBundle(Long id, String username);
	
//	FundDemand updateFundDemand(Long id, FundDemandRequestDto dto, HttpServletRequest request);

}
