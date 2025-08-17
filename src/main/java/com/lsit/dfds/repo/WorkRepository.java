package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {
	List<Work> findByScheme_Id(Long schemeId);

	List<Work> findByVendor_Id(Long vendorId);

	List<Work> findByScheme_IdAndFinancialYear(Long schemeId, String financialYear);

	List<Work> findByScheme_IdAndFinancialYearAndScheme_Districts_Id(Long schemeId, String financialYear,
			Long finalDistrictId);

	Long countByScheme_Districts_IdAndVendorIsNotNull(Long districtId);

	Long countByScheme_Districts_IdAndVendorIsNull(Long districtId);

	@Query("SELECT w FROM Work w WHERE w.district.id = :districtId "
			+ "AND (:planType IS NULL OR w.scheme.planType = :planType) "
			+ "AND (:schemeId IS NULL OR w.scheme.id = :schemeId) "
			+ "AND (:iaUserId IS NULL OR w.createdBy = (SELECT u.username FROM User u WHERE u.id = :iaUserId)) "
			+ "AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%',:search,'%')))")
	List<Work> findByDistrictFilters(Long districtId, String planType, Long schemeId, Long iaUserId, String search);

	@Query("""
			    SELECT w FROM Work w
			    LEFT JOIN FETCH w.scheme s
			    LEFT JOIN FETCH w.district d
			    LEFT JOIN FETCH w.recommendedByMla mla
			    LEFT JOIN FETCH w.recommendedByMlc mlc
			    WHERE (:planType IS NULL OR s.planType = :planType)
			      AND (:schemeId IS NULL OR s.id = :schemeId)
			      AND (:districtId IS NULL OR d.id = :districtId)
			      AND (:iaUserId IS NULL OR w.vendor.id = :iaUserId)
			      AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findWorksWithFilters(@Param("planType") PlanType planType, @Param("schemeId") Long schemeId,
			@Param("districtId") Long districtId, @Param("iaUserId") Long iaUserId, @Param("search") String search);

	List<Work> findByRecommendedByMla_Id(Long mlaId);

	@Query("""
			  SELECT w FROM Work w
			  WHERE (:mlaId IS NULL OR w.recommendedByMla.id = :mlaId)
			    AND (:districtId IS NULL OR w.district.id = :districtId)
			    AND (:constituencyId IS NULL OR w.constituency.id = :constituencyId)
			    AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findWorksForMlaDashboard(@Param("mlaId") Long mlaId, @Param("districtId") Long districtId,
			@Param("constituencyId") Long constituencyId, @Param("search") String search);

	long countByRecommendedByMla_Id(Long mlaId);

	long countByRecommendedByMla_IdAndStatus(Long mlaId, WorkStatuses status);

	@Query("""
			  SELECT w FROM Work w
			  LEFT JOIN FETCH w.scheme s
			  LEFT JOIN FETCH w.district d
			  LEFT JOIN FETCH w.vendor v
			  WHERE (:mlcId IS NULL OR w.recommendedByMlc.id = :mlcId)
			    AND (:districtId IS NULL OR d.id = :districtId)
			    AND (:financialYearId IS NULL OR s.financialYear.id = :financialYearId)
			    AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findWorksForMlcDashboard(@Param("mlcId") Long mlcId, @Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("search") String search);

	@Query("""
			SELECT w
			FROM Work w
			LEFT JOIN w.hadp h
			LEFT JOIN h.taluka t
			LEFT JOIN h.district d
			WHERE h IS NOT NULL
			  AND (:hadpId IS NULL OR h.id = :hadpId)
			  AND (:districtId IS NULL OR d.id = :districtId)
			  AND (:talukaId IS NULL OR t.id = :talukaId)
			  AND (:financialYearId IS NULL OR h.financialYear.id = :financialYearId)
			  AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%',:search,'%'))
			       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%',:search,'%')))
			""")
	List<Work> findWorksForHadpDashboard(@Param("hadpId") Long hadpId, @Param("districtId") Long districtId,
			@Param("talukaId") Long talukaId, @Param("financialYearId") Long financialYearId,
			@Param("search") String search);

}
