// src/main/java/com/lsit/dfds/service/MlcAdminService.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlcInfoDto;
import com.lsit.dfds.dto.MlcListFilterDto;
import com.lsit.dfds.dto.MlcStatusUpdateDto;
import com.lsit.dfds.dto.MlcUpdateDto;
import com.lsit.dfds.enums.Statuses;

public interface MlcAdminService {
	List<MlcInfoDto> list(String username, MlcListFilterDto filter);

	MlcInfoDto getOne(String username, Long id);

	MlcInfoDto update(String username, Long id, MlcUpdateDto dto);

	String setStatus(String username, Long id, Statuses status);

	String setStatus(String username, Long id, Statuses status, MlcStatusUpdateDto dto, MultipartFile letter);
}
