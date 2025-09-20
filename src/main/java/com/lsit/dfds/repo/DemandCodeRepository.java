package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.enums.PlanType;

@Repository
public interface DemandCodeRepository extends JpaRepository<DemandCode, Long> {

	// Find SchemeType by its name or any other unique field
	// If SchemeType has a 'name' field

	// Find all SchemeTypes
	@Override
	List<DemandCode> findAll();

	// List<SchemeType> findAllByDistrictId(Long districtId);
	// You can add more custom query methods here if needed

	Optional<DemandCode> findSchemeTypeById(Long schemeTypeId);

	List<DemandCode> findAllByDistricts_Id(Long id);

	@Query("""
			   SELECT DISTINCT dc FROM DemandCode dc
			     JOIN dc.districts d
			     JOIN dc.schemes s
			   WHERE d.id = :districtId
			     AND dc.financialYear.id = :financialYearId
			     AND (:planType IS NULL OR s.planType = :planType)
			   ORDER BY dc.schemeType
			""")
	List<DemandCode> findForDistrictFyAndPlanType(@Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("planType") PlanType planType);

}