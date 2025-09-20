package com.lsit.dfds.service;

import java.util.List;

import com.lsit.dfds.dto.SchemeRespDto;
import com.lsit.dfds.dto.SchemerequestDto;

public interface ISchemeService {

	List<SchemeRespDto> getAllScheme(String username, SchemerequestDto request);

}
