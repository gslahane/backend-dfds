package com.lsit.dfds.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.FundDemand;
import com.lsit.dfds.enums.DemandStatuses;

@Repository
public interface FundDemandRepository extends JpaRepository<FundDemand, Long> {
	List<FundDemand> findByWork_Id(Long workId);

	List<FundDemand> findByVendor_Id(Long vendorId);

	FundDemand findByDemandId(String demandId);

	List<FundDemand> findByWork_Scheme_Districts_Id(Long id);

//	Optional<FundTransfer> findByFundDemand_Id(Long fundDemandId);

	List<FundDemand> findByCreatedBy(String username);

	List<FundDemand> findByIsDeleteFalse();

	List<FundDemand> findByCreatedByAndIsDeleteFalse(String createdBy);

	@Query("SELECT fd FROM FundDemand fd " + "JOIN fd.work w " + "JOIN w.scheme s " + "JOIN s.districts d "
			+ "WHERE d.id = :districtId "
			+ "AND (:financialYear IS NULL OR fd.financialYear.finacialYear = :financialYear) "
			+ "AND (:planType IS NULL OR s.planType = :planType) " + "AND (:schemeId IS NULL OR s.id = :schemeId) "
			+ "AND (:workId IS NULL OR w.id = :workId) " + "AND (:status IS NULL OR fd.status = :status) "
			+ "AND fd.isDelete = false")
	List<FundDemand> findFilteredDemands(@Param("districtId") Long districtId,
			@Param("financialYear") String financialYear, @Param("planType") String planType,
			@Param("schemeId") Long schemeId, @Param("workId") Long workId, @Param("status") String status);

	@Query("""
			  SELECT fd FROM FundDemand fd
			  JOIN fd.work w
			  WHERE w.recommendedByMla.id = :mlaId
			    AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			    AND (:districtId IS NULL OR w.district.id = :districtId)
			    AND (:constituencyId IS NULL OR w.constituency.id = :constituencyId)
			    AND (:search IS NULL OR LOWER(fd.demandId) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<FundDemand> findDemandsForMla(@Param("mlaId") Long mlaId, @Param("financialYearId") Long financialYearId,
			@Param("districtId") Long districtId, @Param("constituencyId") Long constituencyId,
			@Param("search") String search);

	@Query("""
			  SELECT COALESCE(SUM(fd.netPayable),0) FROM FundDemand fd
			  WHERE fd.work.id = :workId
			    AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			    AND fd.status IN (
			      com.lsit.dfds.enums.DemandStatuses.APPROVED_BY_STATE,
			      com.lsit.dfds.enums.DemandStatuses.APPROVED_BY_DISTRICT,
			      com.lsit.dfds.enums.DemandStatuses.TRANSFERRED
			    )
			""")
	Double sumApprovedNetPayableByWork(@Param("workId") Long workId, @Param("financialYearId") Long financialYearId);

	@Query("""
			  SELECT fd FROM FundDemand fd
			  JOIN fd.work w
			  WHERE (:mlcId IS NULL OR w.recommendedByMlc.id = :mlcId)
			    AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			    AND (:districtId IS NULL OR w.district.id = :districtId)
			    AND (fd.isDelete = false OR fd.isDelete IS NULL)
			    AND (:search IS NULL OR LOWER(fd.demandId) LIKE LOWER(CONCAT('%', :search, '%'))
			                       OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :search, '%')))
			""")
	List<FundDemand> findDemandsForMlc(@Param("mlcId") Long mlcId, @Param("financialYearId") Long financialYearId,
			@Param("districtId") Long districtId, @Param("search") String search);

	@Query("""
			  SELECT COALESCE(SUM(fd.netPayable),0) FROM FundDemand fd
			  WHERE fd.work.id = :workId
			    AND fd.status IN :accepted
			    AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			""")
	Double sumNetPayableByWorkInStatuses(@Param("workId") Long workId, @Param("financialYearId") Long financialYearId,
			@Param("accepted") Collection<DemandStatuses> accepted);

	@Query("""
			SELECT fd
			FROM FundDemand fd
			JOIN fd.work w
			LEFT JOIN w.hadp h
			LEFT JOIN h.taluka t
			LEFT JOIN h.district d
			WHERE h IS NOT NULL
			  AND (:hadpId IS NULL OR h.id = :hadpId)
			  AND (:districtId IS NULL OR d.id = :districtId)
			  AND (:talukaId IS NULL OR t.id = :talukaId)
			  AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			  AND (:search IS NULL OR LOWER(fd.demandId) LIKE LOWER(CONCAT('%',:search,'%'))
			       OR LOWER(w.workName) LIKE LOWER(CONCAT('%',:search,'%')))
			ORDER BY fd.createdAt DESC
			""")
	List<FundDemand> findDemandsForHadp(@Param("hadpId") Long hadpId, @Param("financialYearId") Long financialYearId,
			@Param("districtId") Long districtId, @Param("talukaId") Long talukaId, @Param("search") String search);

	@Query("""
			SELECT COALESCE(SUM(fd.netPayable),0)
			FROM FundDemand fd
			WHERE fd.work.id = :workId
			  AND fd.status IN :statuses
			  AND (:financialYearId IS NULL OR fd.financialYear.id = :financialYearId)
			""")
	Double sumNetPayableByWorkInStatuses(@Param("workId") Long workId, @Param("financialYearId") Long financialYearId,
			@Param("statuses") List<DemandStatuses> statuses);

	@Query("""
			  SELECT d FROM FundDemand d
			  LEFT JOIN d.financialYear fy
			  LEFT JOIN d.work w
			  LEFT JOIN w.district dist
			  WHERE d.isDelete = FALSE
			    AND (:financialYearId IS NULL OR fy.id = :financialYearId)
			    AND (:districtId     IS NULL OR dist.id = :districtId)
			    AND (:createdBy      IS NULL OR d.createdBy = :createdBy)
			    AND (:status         IS NULL OR d.status = :status)
			    AND (
			         :q IS NULL
			         OR LOWER(d.demandId) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(d.vendor.name) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :q, '%'))
			    )
			  ORDER BY d.createdAt DESC
			""")
	List<FundDemand> findFiltered(@Param("financialYearId") Long financialYearId, @Param("districtId") Long districtId,
			@Param("createdBy") String createdBy, @Param("status") DemandStatuses status, @Param("q") String q);
}
