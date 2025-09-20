package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.HADPBudget;

@Repository
public interface HADPBudgetRepository extends JpaRepository<HADPBudget, Long> {

    // 🔹 Unique budget allocation per HADP + Taluka + FY
    Optional<HADPBudget> findByHadp_IdAndTaluka_IdAndFinancialYear_Id(Long hadpId, Long talukaId, Long fyId);

    // 🔹 Direct budget lookup by Taluka + FY
    Optional<HADPBudget> findByTaluka_IdAndFinancialYear_Id(Long talukaId, Long fyId);


    // 🔹 Budget lookup by Taluka + Scheme + FY (for scheme-level breakdowns)
    Optional<HADPBudget> findByTaluka_IdAndScheme_IdAndFinancialYear_Id(Long talukaId, Long schemeId, Long fyId);

    // 🔹 By District / Taluka / FY
    List<HADPBudget> findByDistrict_Id(Long districtId);
    List<HADPBudget> findByTaluka_Id(Long talukaId);
    List<HADPBudget> findByFinancialYear_Id(Long fyId);
    List<HADPBudget> findByDistrict_IdAndTaluka_Id(Long districtId, Long talukaId);
    List<HADPBudget> findByHadp_Id(Long hadpId);
    List<HADPBudget> findByHadp_IdAndFinancialYear_Id(Long hadpId, Long financialYearId);
    List<HADPBudget> findByFinancialYear_IdAndDistrict_Id(Long financialYearId, Long districtId);

    // 🔹 Dynamic search with optional filters
    @Query("""
            select b from HADPBudget b
             where (:hadpId is null or b.hadp.id = :hadpId)
               and (:districtId is null or b.district.id = :districtId)
               and (:talukaId is null or b.taluka.id = :talukaId)
               and (:financialYearId is null or b.financialYear.id = :financialYearId)
               and (:schemeId is null or b.scheme.id = :schemeId)
               and (b.status is null or b.status = com.lsit.dfds.enums.Statuses.ACTIVE)
           """)
    List<HADPBudget> findBudgets(@Param("hadpId") Long hadpId,
                                 @Param("districtId") Long districtId,
                                 @Param("talukaId") Long talukaId,
                                 @Param("financialYearId") Long financialYearId,
                                 @Param("schemeId") Long schemeId);

    // 🔹 Aggregation queries
    @Query("select coalesce(sum(b.allocatedLimit),0) from HADPBudget b")
    Double sumAllocatedAllFys();

    @Query("select coalesce(sum(b.allocatedLimit),0) from HADPBudget b where b.financialYear.id = :financialYearId")
    Double sumAllocatedByFy(@Param("financialYearId") Long financialYearId);

    @Query("""
            select coalesce(sum(b.allocatedLimit),0)
            from HADPBudget b
            where (:fyId is null or b.financialYear.id = :fyId)
           """)
    Double sumAllocatedByFyNullable(@Param("fyId") Long fyId);

    @Query("""
            select coalesce(sum(b.allocatedLimit),0)
            from HADPBudget b
            where (:talukaId is null or b.taluka.id = :talukaId)
              and (:fyId is null or b.financialYear.id = :fyId)
           """)
    Double sumAllocatedByTalukaAndFyNullable(@Param("talukaId") Long talukaId,
                                             @Param("fyId") Long fyId);

    @Query("""
            select coalesce(sum(b.utilizedLimit),0)
            from HADPBudget b
            where (:talukaId is null or b.taluka.id = :talukaId)
              and (:fyId is null or b.financialYear.id = :fyId)
           """)
    Double sumUtilizedByTalukaAndFyNullable(@Param("talukaId") Long talukaId,
                                            @Param("fyId") Long fyId);
}
