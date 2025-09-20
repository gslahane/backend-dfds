// src/main/java/com/lsit/dfds/service/MlaAdminService.java
package com.lsit.dfds.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.lsit.dfds.dto.MlaInfoDto;
import com.lsit.dfds.dto.MlaListFilterDto;
import com.lsit.dfds.dto.MlaStatusUpdateDto;
import com.lsit.dfds.dto.MlaUpdateDto;

public interface MlaAdminService {
	List<MlaInfoDto> list(String username, MlaListFilterDto filter);

	MlaInfoDto getOne(String username, Long mlaId);

	MlaInfoDto update(String username, Long mlaId, MlaUpdateDto dto);

	MlaInfoDto updateStatus(String username, Long mlaId, MlaStatusUpdateDto dto);

	MlaInfoDto updateStatus(String username, Long mlaId, MlaStatusUpdateDto dto, MultipartFile letter);
}
