package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.SchemeReappropriation;
import com.lsit.dfds.enums.ReappropriationStatuses;

@Repository
public interface SchemeReappropriationRepository extends JpaRepository<SchemeReappropriation, Long> {
	List<SchemeReappropriation> findByDistrict_Id(Long districtId);

	@Query("SELECT r FROM SchemeReappropriation r WHERE r.status = :status")
	List<SchemeReappropriation> findByStatus(@Param("status") ReappropriationStatuses status);
}
