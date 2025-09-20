// src/main/java/com/lsit/dfds/repo/MLCTermRepository.java
package com.lsit.dfds.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.MLCTerm;

public interface MLCTermRepository extends JpaRepository<MLCTerm, Long> {
	Optional<MLCTerm> findFirstByMlc_IdAndActiveTrue(Long mlcId);
}
