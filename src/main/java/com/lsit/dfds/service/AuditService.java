package com.lsit.dfds.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lsit.dfds.entity.AuditLog;
import com.lsit.dfds.repo.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

	private final AuditLogRepository repo;
	private final ObjectMapper objectMapper; // Spring Boot auto-configures this

	public void log(String actor, String action, String entityType, Long entityId, String detailsJson) {
		AuditLog a = new AuditLog();
		a.setActorUsername(actor);
		a.setAction(action);
		a.setEntityType(entityType);
		a.setEntityId(entityId);
		a.setDetailsJson(detailsJson);
		repo.save(a);
	}

	public void log(String actor, String action, String entityType, Long entityId, Object details) {
		String json = null;
		if (details != null) {
			try {
				json = objectMapper.writeValueAsString(details);
			} catch (Exception e) {
				json = String.valueOf(details);
			}
		}
		log(actor, action, entityType, entityId, json);
	}
}
