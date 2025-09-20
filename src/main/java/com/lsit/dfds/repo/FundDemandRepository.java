package com.lsit.dfds.repo;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	List<FundDemand> findByIsDeleteFalseAndStatusIn(Collection<DemandStatuses> statuses);

//	Optional<FundTransfer> findByFundDemand_Id(Long fundDemandId);

	List<FundDemand> findByWork_IdOrderByDemandDateDesc(Long workId);

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

	// District-wide demands, filtered
	@Query("""
			  select d from FundDemand d
			  where d.isDelete = false
			    and d.work.district.id = :districtId
			    and (:fyId is null or d.financialYear.id = :fyId)
			    and (:mlaId is null or d.work.recommendedByMla.id = :mlaId)
			    and (:mlcId is null or d.work.recommendedByMlc.id = :mlcId)
			    and (:status is null or d.status = :status)
			""")
	List<FundDemand> findDistrictDemands(@Param("districtId") Long districtId, @Param("fyId") Long fyId,
			@Param("mlaId") Long mlaId, @Param("mlcId") Long mlcId, @Param("status") DemandStatuses status);

	// Amount/Count helpers (district scope)
	@Query("""
			  select coalesce(sum(d.netPayable),0) from FundDemand d
			  where d.isDelete=false and d.work.district.id=:districtId
			    and (:fyId is null or d.financialYear.id=:fyId)
			    and d.status=:status
			""")
	Double sumByDistrictAndStatus(@Param("districtId") Long districtId, @Param("fyId") Long fyId,
			@Param("status") DemandStatuses status);

	@Query("""
			  select count(d) from FundDemand d
			  where d.isDelete=false and d.work.district.id=:districtId
			    and (:fyId is null or d.financialYear.id=:fyId)
			    and d.status=:status
			""")
	Long countByDistrictAndStatus(@Param("districtId") Long districtId, @Param("fyId") Long fyId,
			@Param("status") DemandStatuses status);

	// Disbursed amount (state transfers)
	@Query("""
			  select coalesce(sum(d.netPayable),0) from FundDemand d
			  where d.isDelete=false and d.work.district.id=:districtId
			    and (:fyId is null or d.financialYear.id=:fyId)
			    and d.status = com.lsit.dfds.enums.DemandStatuses.TRANSFERRED
			""")
	Double sumTransferredByDistrict(@Param("districtId") Long districtId, @Param("fyId") Long fyId);

	// Per MLA/MLC disbursed
	@Query("""
			  select coalesce(sum(d.netPayable),0) from FundDemand d
			  where d.isDelete=false
			    and d.work.district.id=:districtId
			    and (:fyId is null or d.financialYear.id=:fyId)
			    and d.status = com.lsit.dfds.enums.DemandStatuses.TRANSFERRED
			    and d.work.recommendedByMla.id = :mlaId
			""")
	Double sumTransferredForMla(@Param("districtId") Long districtId, @Param("fyId") Long fyId,
			@Param("mlaId") Long mlaId);

	@Query("""
			  select coalesce(sum(d.netPayable),0) from FundDemand d
			  where d.isDelete=false
			    and d.work.district.id=:districtId
			    and (:fyId is null or d.financialYear.id=:fyId)
			    and d.status = com.lsit.dfds.enums.DemandStatuses.TRANSFERRED
			    and d.work.recommendedByMlc.id = :mlcId
			""")
	Double sumTransferredForMlc(@Param("districtId") Long districtId, @Param("fyId") Long fyId,
			@Param("mlcId") Long mlcId);

	@Query("""
			  select fd from FundDemand fd
			    join fd.work w
			  where fd.isDelete = false
			    and fd.vendor.id = :vendorId
			    and (:financialYearId is null or fd.financialYear.id = :financialYearId)
			    and (:districtId is null or w.district.id = :districtId)
			    and (:schemeId is null or w.scheme.id = :schemeId)
			    and (:status is null or fd.status = :status)
			    and (:search is null or lower(w.workName) like lower(concat('%',:search,'%'))
			                     or lower(w.workCode) like lower(concat('%',:search,'%'))
			                     or lower(fd.demandId) like lower(concat('%',:search,'%')))
			  order by fd.createdAt desc
			""")
	List<FundDemand> findDemandsForVendor(@Param("vendorId") Long vendorId,
			@Param("financialYearId") Long financialYearId, @Param("districtId") Long districtId,
			@Param("schemeId") Long schemeId, @Param("status") DemandStatuses status, @Param("search") String search);

	// --- MLA aggregates ---

	// --- MLC aggregates ---

	// --- HADP aggregates: via Work -> Taluka ---
	@Query("select coalesce(sum(fd.netPayable),0) " + "from FundDemand fd " + "where fd.work.hadp is not null "
			+ "and fd.work.district.id = (select t.district.id from Taluka t where t.id = :talukaId) "
			+ "and fd.work.implementingAgency.district.id = fd.work.district.id " + // keeps IA within district
																					// (optional)
			"and fd.work.constituency is null " + // keep HADP separation (optional)
			"and fd.work.hadp.taluka.id = :talukaId " + "and fd.status in :statuses")
	Double sumByTalukaInStatuses(Long talukaId, Collection<DemandStatuses> statuses);

	@Query("select coalesce(sum(fd.netPayable),0) " + "from FundDemand fd " + "where fd.work.hadp is not null "
			+ "and fd.work.hadp.taluka.id = :talukaId " + "and fd.financialYear.id = :fyId "
			+ "and fd.status in :statuses")
	Double sumByTalukaAndFyInStatuses(Long talukaId, Long fyId, Collection<DemandStatuses> statuses);

	// ----- MLA sums & counts -----
	@Query("""
			    select coalesce(sum(fd.netPayable),0) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMla.id = :mlaId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Double sumByMlaAndFyInStatuses(@Param("mlaId") Long mlaId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select coalesce(sum(fd.netPayable),0) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMla.id = :mlaId
			      and fd.status in :statuses
			""")
	Double sumByMlaInStatuses(@Param("mlaId") Long mlaId, @Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select count(fd) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMla.id = :mlaId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Long countByMlaAndFyInStatuses(@Param("mlaId") Long mlaId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select count(fd) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMla.id = :mlaId
			      and fd.status in :statuses
			""")
	Long countByMlaInStatuses(@Param("mlaId") Long mlaId, @Param("statuses") Collection<DemandStatuses> statuses);

	// ----- MLC sums & counts -----
	@Query("""
			    select coalesce(sum(fd.netPayable),0) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMlc.id = :mlcId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Double sumByMlcAndFyInStatuses(@Param("mlcId") Long mlcId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select coalesce(sum(fd.netPayable),0) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMlc.id = :mlcId
			      and fd.status in :statuses
			""")
	Double sumByMlcInStatuses(@Param("mlcId") Long mlcId, @Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select count(fd) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMlc.id = :mlcId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Long countByMlcAndFyInStatuses(@Param("mlcId") Long mlcId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select count(fd) from FundDemand fd
			    join fd.work w
			    where w.recommendedByMlc.id = :mlcId
			      and fd.status in :statuses
			""")
	Long countByMlcInStatuses(@Param("mlcId") Long mlcId, @Param("statuses") Collection<DemandStatuses> statuses);

	// ----- HADP sums & counts (by Taluka) -----
	@Query("""
			    select coalesce(sum(fd.netPayable),0) from FundDemand fd
			    join fd.work w
			    where w.hadp.id is not null
			      and w.hadp.taluka.id = :talukaId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Double sumByHadpTalukaAndFyInStatuses(@Param("talukaId") Long talukaId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	@Query("""
			    select count(fd) from FundDemand fd
			    join fd.work w
			    where w.hadp.id is not null
			      and w.hadp.taluka.id = :talukaId
			      and fd.status in :statuses
			      and (:fyId is null or fd.financialYear.id = :fyId)
			""")
	Long countByHadpTalukaAndFyInStatuses(@Param("talukaId") Long talukaId, @Param("fyId") Long fyId,
			@Param("statuses") Collection<DemandStatuses> statuses);

	// -------- click-through pages ---------

	@Query("""
			    select fd from FundDemand fd
			    join fd.work w
			    where w.recommendedByMla.id = :mlaId
			      and fd.status in (com.lsit.dfds.enums.DemandStatuses.PENDING, com.lsit.dfds.enums.DemandStatuses.APPROVED_BY_STATE)
			      and (:fyId is null or fd.financialYear.id = :fyId)
			      and (:districtId is null or w.district.id = :districtId)
			      and (
			            :q is null or :q = '' or
			            lower(fd.demandId) like concat('%', lower(:q), '%') or
			            lower(w.workName)  like concat('%', lower(:q), '%') or
			            lower(w.workCode)  like concat('%', lower(:q), '%') or
			            (w.vendor is not null and lower(w.vendor.name) like concat('%', lower(:q), '%'))
			          )
			""")
	Page<FundDemand> findPendingByMla(@Param("mlaId") Long mlaId, @Param("fyId") Long fyId,
			@Param("districtId") Long districtId, @Param("q") String search, Pageable pageable);

	@Query("""
			    select fd from FundDemand fd
			    join fd.work w
			    where w.recommendedByMlc.id = :mlcId
			      and fd.status in (com.lsit.dfds.enums.DemandStatuses.PENDING, com.lsit.dfds.enums.DemandStatuses.APPROVED_BY_STATE)
			      and (:fyId is null or fd.financialYear.id = :fyId)
			      and (:districtId is null or w.district.id = :districtId)
			      and (
			            :q is null or :q = '' or
			            lower(fd.demandId) like concat('%', lower(:q), '%') or
			            lower(w.workName)  like concat('%', lower(:q), '%') or
			            lower(w.workCode)  like concat('%', lower(:q), '%') or
			            (w.vendor is not null and lower(w.vendor.name) like concat('%', lower(:q), '%'))
			          )
			""")
	Page<FundDemand> findPendingByMlc(@Param("mlcId") Long mlcId, @Param("fyId") Long fyId,
			@Param("districtId") Long districtId, @Param("q") String search, Pageable pageable);

	@Query("""
			    select fd from FundDemand fd
			    join fd.work w
			    where w.hadp.id is not null
			      and w.hadp.taluka.id = :talukaId
			      and fd.status in (com.lsit.dfds.enums.DemandStatuses.PENDING, com.lsit.dfds.enums.DemandStatuses.APPROVED_BY_STATE)
			      and (:fyId is null or fd.financialYear.id = :fyId)
			      and (:districtId is null or w.district.id = :districtId)
			      and (
			            :q is null or :q = '' or
			            lower(fd.demandId) like concat('%', lower(:q), '%') or
			            lower(w.workName)  like concat('%', lower(:q), '%') or
			            lower(w.workCode)  like concat('%', lower(:q), '%') or
			            (w.vendor is not null and lower(w.vendor.name) like concat('%', lower(:q), '%'))
			          )
			""")
	Page<FundDemand> findPendingByHadpTaluka(@Param("talukaId") Long talukaId, @Param("fyId") Long fyId,
			@Param("districtId") Long districtId, @Param("q") String search, Pageable pageable);

}
