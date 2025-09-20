package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long>, JpaSpecificationExecutor<Work> {

	List<Work> findByScheme_Id(Long schemeId);

	List<Work> findByVendor_Id(Long vendorId);

	// ✅ Use FY *ID* (Work.financialYear is an entity now)
	List<Work> findByScheme_IdAndFinancialYear_Id(Long schemeId, Long financialYearId);

	List<Work> findByScheme_IdAndFinancialYear_IdAndDistrict_Id(Long schemeId, Long financialYearId, Long districtId);

	// (Optional) If you ever need to filter by the FY *label* string:
	List<Work> findByScheme_IdAndFinancialYear_FinacialYearAndDistrict_Id(Long schemeId, String finacialYear,
			Long districtId);

	// ✅ Count by Work.district (unless your Schemes actually has a "districts"
	// relation)
	Long countByDistrict_IdAndVendorIsNotNull(Long districtId);

	Long countByDistrict_IdAndVendorIsNull(Long districtId);

	List<Work> findByHadp_Id(Long hadpId);

	List<Work> findByHadp_IdAndFinancialYear_Id(Long hadpId, Long financialYearId);

	// New: narrow to vendor-assigned + specific status
	List<Work> findByVendorIsNotNullAndStatus(WorkStatuses status);

	List<Work> findByDistrict_IdAndVendorIsNotNullAndStatus(Long districtId, WorkStatuses status);

	List<Work> findByImplementingAgency_IdAndVendorIsNotNullAndStatus(Long iaUserId, WorkStatuses status);

	@Query("""
			SELECT w FROM Work w
			WHERE w.district.id = :districtId
			  AND (:planType IS NULL OR w.scheme.planType = :planType)
			  AND (:schemeId IS NULL OR w.scheme.id = :schemeId)
			  AND (:iaUserId IS NULL OR w.implementingAgency.id = :iaUserId)
			  AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findByDistrictFilters(@Param("districtId") Long districtId, @Param("planType") PlanType planType,
			@Param("schemeId") Long schemeId, @Param("iaUserId") Long iaUserId, @Param("search") String search);

	@Query("""
			SELECT w FROM Work w
			LEFT JOIN FETCH w.scheme s
			LEFT JOIN FETCH w.district d
			LEFT JOIN FETCH w.recommendedByMla mla
			LEFT JOIN FETCH w.recommendedByMlc mlc
			WHERE (:planType IS NULL OR s.planType = :planType)
			  AND (:schemeId IS NULL OR s.id = :schemeId)
			  AND (:districtId IS NULL OR d.id = :districtId)
			  AND (:iaUserId IS NULL OR w.implementingAgency.id = :iaUserId)
			  AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findWorksWithFilters(@Param("planType") PlanType planType, @Param("schemeId") Long schemeId,
			@Param("districtId") Long districtId, @Param("iaUserId") Long iaUserId, @Param("search") String search);

	List<Work> findByRecommendedByMla_Id(Long mlaId);

	@Query("""
			SELECT w FROM Work w
			WHERE (:mlaId IS NULL OR w.recommendedByMla.id = :mlaId)
			  AND (:districtId IS NULL OR w.district.id = :districtId)
			  AND (:constituencyId IS NULL OR w.constituency.id = :constituencyId)
			  AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :search, '%')))
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
	Page<Work> findWorksForMlcDashboard(@Param("mlcId") Long mlcId, @Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("search") String search, Pageable pageable);

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
			  AND (:search IS NULL OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<Work> findWorksForHadpDashboard(@Param("hadpId") Long hadpId, @Param("districtId") Long districtId,
			@Param("talukaId") Long talukaId, @Param("financialYearId") Long financialYearId,
			@Param("search") String search);

	// Common helpers
	List<Work> findByDistrict_Id(Long districtId);

	List<Work> findByRecommendedByMlc_Id(Long mlcId);

	List<Work> findByImplementingAgency_Id(Long iaUserId);

	List<Work> findByFinancialYear_Id(Long financialYearId);

	@Query("""
			select w from Work w
			where w.district.id = :districtId
			  and (:mlaId is null or w.recommendedByMla.id = :mlaId)
			  and (:mlcId is null or w.recommendedByMlc.id = :mlcId)
			  and (:fyId is null or w.financialYear.id = :fyId)
			""")
	List<Work> findDistrictWorks(@Param("districtId") Long districtId, @Param("mlaId") Long mlaId,
			@Param("mlcId") Long mlcId, @Param("fyId") Long fyId);

	@Query("""
			select w from Work w
			where w.vendor.id = :vendorId
			  and (:districtId is null or w.district.id = :districtId)
			  and (:schemeId is null or w.scheme.id = :schemeId)
			  and (:search is null or lower(w.workName) like lower(concat('%', :search, '%'))
			                     or lower(w.workCode) like lower(concat('%', :search, '%')))
			order by w.createdAt desc
			""")
	List<Work> findWorksForVendor(@Param("vendorId") Long vendorId, @Param("districtId") Long districtId,
			@Param("schemeId") Long schemeId, @Param("search") String search);

	@Query("""
			  select count(w) from Work w
			  where w.district.id = :districtId
			    and (:schemeId is null or w.scheme.id = :schemeId)
			    and (:fyId    is null or w.financialYear.id = :fyId)
			    and (:mapped  is null or ( :mapped = true and w.vendor is not null )
			                       or ( :mapped = false and w.vendor is null ))
			""")
	long countVendorMapping(@Param("districtId") Long districtId, @Param("schemeId") Long schemeId,
			@Param("fyId") Long financialYearId, @Param("mapped") Boolean mapped);

	Long countByDistrict_IdAndScheme_IdAndVendorIsNotNull(Long districtId, Long schemeId);

	Long countByDistrict_IdAndFinancialYear_IdAndVendorIsNotNull(Long districtId, Long financialYearId);

	// Find works by status
	List<Work> findByStatus(WorkStatuses status);

	Page<Work> findAll(Specification<Work> spec, Pageable pageable);

	long countByDistrict_IdAndAdminApprovedAmountGreaterThan(Long districtId, double min);

	long countByDistrict_IdAndAdminApprovedAmountGreaterThanAndVendorIsNull(Long districtId, double min);

	@Query("""
			    select coalesce(sum(w.adminApprovedAmount), 0)
			    from Work w
			    where w.district.id = :districtId
			      and w.adminApprovedAmount > 0
			""")
	Double sumAdminApprovedAmountByDistrict(@Param("districtId") Long districtId);

}