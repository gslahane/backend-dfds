package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.SchemeBudget;

public interface SchemeBudgetRepository extends JpaRepository<SchemeBudget, Long> {

	// Custom method to find SchemeBudget by financialYear ID and scheme ID
	SchemeBudget findByFinancialYear_IdAndScheme_Id(Long financialYearId, Long schemeId);

	Optional<SchemeBudget> findByDistrict_IdAndScheme_IdAndFinancialYear_Id(Long districtId, Long schemeId, Long fyId);

//	Optional<SchemeBudget> findByDistrict_IdAndScheme_IdAndFinancialYear_Id(Long districtId, Long schemeId, Long financialYearId);

	@Query("""
			   SELECT sb FROM SchemeBudget sb
			   JOIN sb.scheme s
			   WHERE sb.district.id = :districtId
			     AND sb.financialYear.id = :financialYearId
			     AND s.schemeType.id = :demandCodeId
			""")
	List<SchemeBudget> findForDistrictFyAndDemandCode(@Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("demandCodeId") Long demandCodeId);

//	  Optional<SchemeBudget> findByDistrict_IdAndScheme_IdAndFinancialYear_Id(Long districtId, Long schemeId, Long fyId);

}