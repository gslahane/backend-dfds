package com.lsit.dfds.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.MLATerm;

public interface MLATermRepository extends JpaRepository<MLATerm, Long> {

	Optional<MLATerm> findFirstByMla_IdAndActiveTrue(Long mlaId);

}
