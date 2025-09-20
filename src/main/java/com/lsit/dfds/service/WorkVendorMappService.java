package com.lsit.dfds.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.AssignVendorRequestDto;
import com.lsit.dfds.dto.IaDashboardCountsDto;
import com.lsit.dfds.dto.PagedResponse;
import com.lsit.dfds.dto.WorkRowDto;
import com.lsit.dfds.entity.Vendor;
import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;

public interface WorkVendorMappService {
	List<Work> getWorksForIAUser(String username, String financialYear, Long schemeId, String status);

	List<Vendor> getVendorsForIA(String username);

//	Work assignVendorToWork(AssignVendorRequestDto dto, String username);

	Work assignVendorToWork(AssignVendorRequestDto dto, String username, MultipartFile workOrderFile);

	IaDashboardCountsDto getDashboardCounts(String username);

//	Map<String, Long> getDashboardCounts(String username);

	PagedResponse<WorkRowDto> getAaApprovedWorks(String username, PlanType planType, // MLA / MLC / HADP (required)
			Long financialYearId, // optional
			Long schemeId, // optional
			Integer page, // default 0
			Integer size // default 50
	);
}
