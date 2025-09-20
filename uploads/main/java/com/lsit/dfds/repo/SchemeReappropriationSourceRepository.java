package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.SchemeReappropriationSource;

@Repository
public interface SchemeReappropriationSourceRepository extends JpaRepository<SchemeReappropriationSource, Long> {
	List<SchemeReappropriationSource> findByReappropriation_Id(Long reappropriationId);
}
