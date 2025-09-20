package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.AssignWorkRequestDto;
import com.lsit.dfds.dto.IdNameDto;
import com.lsit.dfds.dto.WorkDetailsDto;
import com.lsit.dfds.entity.Work;

public interface SchemeWorkMasterService {
//	List<Schemes> getSchemesForDistrict(String username, String planType);
//
//	List<User> getIAUsers(String username);

	Work assignWork(AssignWorkRequestDto dto, String username);

	List<WorkDetailsDto> getAssignedWorks(String username, String planType, Long schemeId, Long iaUserId,
			String search);

	List<IdNameDto> getSchemesForDistrict(String username, String planType, Long districtId);

	List<IdNameDto> getIAUsers(String username, Long districtId);

}
