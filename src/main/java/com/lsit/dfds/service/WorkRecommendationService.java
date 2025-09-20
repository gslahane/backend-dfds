package com.lsit.dfds.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.HADPRecommendWorkReqDto;
import com.lsit.dfds.dto.HadpRecomendWorkResponseDto;
import com.lsit.dfds.dto.HadpWorkDetailsDto;
import com.lsit.dfds.dto.MLARecomendWorkReqDto;
import com.lsit.dfds.dto.MLARecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecomendWorkResponseDto;
import com.lsit.dfds.dto.MLCRecommendWorkReqDto;
import com.lsit.dfds.dto.WorkAaApprovalDto;
import com.lsit.dfds.dto.WorkApprovalDto;
import com.lsit.dfds.dto.WorkDetailsWithStatusDto;
import com.lsit.dfds.dto.WorkListFilter;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.HadpType;
import com.lsit.dfds.enums.WorkStatuses;

public interface WorkRecommendationService {

	// MLA
	MLARecomendWorkResponseDto recommendByMla(MLARecomendWorkReqDto dto, String username, String workCode,
			String recommendationLetterUrl);

	// MLC
//	MLCRecomendWorkResponseDto recommendByMlc(WorkRecommendDto dto, String username, MultipartFile letter);

	// HADP
//	HadpRecomendWorkResponseDto recommendByHadp(WorkRecommendDto dto, String username, MultipartFile letter);

	// District approval workflow
	Work approveByAdpo(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl);

	Work approveByDpo(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl);

	Work approveByCollector(WorkApprovalDto dto, String approverUsername, String approvalLetterUrl);

	Work rejectWork(WorkApprovalDto dto, String rejectorUsername, String rejectionLetterUrl);

	// AA + IA assign (and budget impact)
	Work approveAaAndAssign(WorkAaApprovalDto dto, String approverUsername, MultipartFile aaLetterFile);

	// Listing
	List<Work> listWorksForUser(String username, WorkListFilter f);

	// Status-wise details
	List<WorkDetailsWithStatusDto> getWorksByStatus(String username, WorkStatuses status, Long districtId, Long mlaId,
			Long mlcId);

//	MLCRecomendWorkResponseDto recommendByMlc(MLCRecommendWorkReqDto dto, String username, MultipartFile letter);

	HadpRecomendWorkResponseDto recommendByHadp(HADPRecommendWorkReqDto dto, String username, MultipartFile letter);

	MLCRecomendWorkResponseDto recommendByMlc(MLCRecommendWorkReqDto dto, String username,
			String recommendationLetterUrl);

	List<HadpWorkDetailsDto> fetchHadpWorks(String username, Long hadpId, Long districtId, Long talukaId,
			Long financialYearId, WorkStatuses status, HadpType hadpType);
}