package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.HADP;
import com.lsit.dfds.enums.HadpType;

public interface HADPRepository extends JpaRepository<HADP, Long> {

    // Fetch a single HADP by District + Taluka + Financial Year
    Optional<HADP> findByDistrict_IdAndTaluka_IdAndFinancialYear_Id(Long districtId, Long talukaId,
                                                                    Long financialYearId);

    // Fetch HADP by District + Financial Year (when no taluka is linked)
    Optional<HADP> findByDistrict_IdAndFinancialYear_IdAndTalukaIsNull(Long districtId, Long financialYearId);

    // ✅ Changed from List to Optional (assuming one HADP per taluka)
    Optional<HADP> findByTaluka_Id(Long talukaId);

    // Fetch active HADP rows with optional filters
    @Query("""
            select h from HADP h
             where (:districtId is null or h.district.id = :districtId)
               and (:financialYearId is null or h.financialYear.id = :financialYearId)
               and (:schemeId is null or h.scheme.id = :schemeId)
               and (:type is null or h.type = :type)
               and (h.status is null or h.status = com.lsit.dfds.enums.Statuses.ACTIVE)
           """)
    List<HADP> findActiveHadpRows(@Param("districtId") Long districtId,
                                  @Param("financialYearId") Long financialYearId,
                                  @Param("schemeId") Long schemeId,
                                  @Param("type") HadpType type);
}
