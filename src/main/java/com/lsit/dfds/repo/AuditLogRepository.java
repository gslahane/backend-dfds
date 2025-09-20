package com.lsit.dfds.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
