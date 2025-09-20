package com.lsit.dfds.service;

import com.lsit.dfds.dto.SchemeCreateDto;
import com.lsit.dfds.dto.SchemeResponseDto;

public interface SchemeCreateService {

	SchemeResponseDto createScheme(SchemeCreateDto dto, String createdBy);

}
