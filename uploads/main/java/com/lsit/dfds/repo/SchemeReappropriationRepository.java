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

	@Query("""
			   SELECT r FROM SchemeReappropriation r
			   JOIN r.district d
			   JOIN r.schemeType dc
			   JOIN r.financialYear fy
			   WHERE (:districtId IS NULL OR d.id = :districtId)
			     AND (:financialYearId IS NULL OR fy.id = :financialYearId)
			     AND (:demandCodeId IS NULL OR dc.id = :demandCodeId)
			     AND (:status IS NULL OR r.status = :status)
			   ORDER BY r.createdAt DESC
			""")
	List<SchemeReappropriation> search(@Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("demandCodeId") Long demandCodeId,
			@Param("status") ReappropriationStatuses status);

}
