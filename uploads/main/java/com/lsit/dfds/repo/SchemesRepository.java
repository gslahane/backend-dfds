package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.Schemes;
import com.lsit.dfds.enums.PlanType;

@Repository
public interface SchemesRepository extends JpaRepository<Schemes, Long> {
	List<Schemes> findBySector_Id(Long sectorId);

	List<Schemes> findByDistricts_Id(Long districtId); // if ManyToMany mapped from both sides

	@Query("SELECT COUNT(s) FROM Schemes s JOIN s.districts d WHERE d.id = :districtId")
	Long countByDistricts_Id(@Param("districtId") Long districtId);

	@Query("SELECT s FROM Schemes s " + "JOIN s.schemeType st " + "JOIN st.districts d "
			+ "WHERE d = :district AND s.planType = :planType")
	List<Schemes> findByDistrictAndPlanType(@Param("district") District district, @Param("planType") PlanType planType);

	List<Schemes> findByPlanType(PlanType planType); // if ManyToMany mapped from both sides
	// Custom query to find Schemes by SchemeType and District ID

	@Query("SELECT s FROM Schemes s JOIN s.districts d WHERE s.schemeType.id = :schemeTypeId AND d.id = :districtId")
	List<Schemes> findSchemesBySchemeTypeAndDistrictId(@Param("schemeTypeId") Long schemeTypeId,
			@Param("districtId") Long districtId);

	// ID filter (DemandCode ID)
	@Query("""
			SELECT DISTINCT s
			FROM Schemes s
			WHERE (:financialYearId IS NULL OR s.financialYear.id = :financialYearId)
			  AND (:planType       IS NULL OR s.planType = :planType)
			  AND (
			       :districtId IS NULL OR EXISTS (
			         SELECT 1 FROM District d JOIN d.schemes ds
			         WHERE d.id = :districtId AND ds = s
			       )
			  )
			  AND (:schemeTypeId  IS NULL OR s.schemeType.id = :schemeTypeId)
			""")
	List<Schemes> findFilteredSchemes(@Param("financialYearId") Long financialYearId,
			@Param("planType") PlanType planType, @Param("districtId") Long districtId,
			@Param("schemeTypeId") Long schemeTypeId);

	// NAME filter (DemandCode.schemeType LIKE %name%)
	@Query("""
			SELECT DISTINCT s
			FROM Schemes s
			WHERE (:financialYearId IS NULL OR s.financialYear.id = :financialYearId)
			  AND (:planType       IS NULL OR s.planType = :planType)
			  AND (
			       :districtId IS NULL OR EXISTS (
			         SELECT 1 FROM District d JOIN d.schemes ds
			         WHERE d.id = :districtId AND ds = s
			       )
			  )
			  AND (:schemeTypeName IS NULL OR LOWER(s.schemeType.schemeType) LIKE LOWER(CONCAT('%', :schemeTypeName, '%')))
			""")
	List<Schemes> findFilteredSchemesByName(@Param("financialYearId") Long financialYearId,
			@Param("planType") PlanType planType, @Param("districtId") Long districtId,
			@Param("schemeTypeName") String schemeTypeName);

	List<Schemes> findByDistricts_IdAndPlanType(Long id, PlanType mla);

	@Query("""
			SELECT DISTINCT s
			FROM Schemes s
			LEFT JOIN s.districts d
			WHERE (:schemeTypeId IS NULL OR s.schemeType.id = :schemeTypeId)
			  AND (:planType IS NULL OR s.planType = :planType)
			  AND (:districtId IS NULL OR d.id = :districtId)
			  AND (:financialYearId IS NULL OR s.financialYear.id = :financialYearId)
			""")
	List<Schemes> findSchemesWithFilters(@Param("schemeTypeId") Long schemeTypeId, @Param("planType") PlanType planType,
			@Param("districtId") Long districtId, @Param("financialYearId") Long financialYearId);
//	List<Schemes> findByDistrictAndPlanType(District district, PlanType planType);

	@Query("SELECT s FROM Schemes s JOIN s.districts d WHERE d = :district")
	List<Schemes> findByDistrict(@Param("district") District district);

	List<Schemes> findByDistrictsContainsAndPlanType(District targetDistrict, PlanType valueOf);

	// ✅ Fetch schemes for a district with PlanType filter
	@Query("""
			    SELECT s FROM Schemes s
			    JOIN s.districts d
			    WHERE d = :district AND (:planType IS NULL OR s.planType = :planType)
			""")
	List<Schemes> findByDistrictAndOptionalPlanType(@Param("district") District district,
			@Param("planType") PlanType planType);

	// ✅ Fetch schemes for district without PlanType
	/*
	 * @Query("SELECT s FROM Schemes s JOIN s.districts d WHERE d = :district")
	 * List<Schemes> findByDistrict(@Param("district") District district);
	 */

	// ✅ Fetch all schemes mapped to a district
	@Query("SELECT s FROM Schemes s JOIN s.districts d WHERE d.id = :districtId")
	List<Schemes> findSchemesByDistrictId(@Param("districtId") Long districtId);

	// ✅ Fetch schemes with optional filters
	@Query("""
			    SELECT DISTINCT s FROM Schemes s
			    JOIN s.districts d
			    WHERE (:districtId IS NULL OR d.id = :districtId)
			    AND (:districtName IS NULL OR LOWER(d.districtName) = LOWER(:districtName))
			    AND (:schemeType IS NULL OR LOWER(s.schemeType.schemeType) = LOWER(:schemeType))
			""")
	List<Schemes> searchSchemesWithFilters(@Param("districtId") Long districtId,
			@Param("districtName") String districtName, @Param("schemeType") String schemeType);

	@Query("""
			SELECT DISTINCT s
			FROM Schemes s
			JOIN s.districts d
			WHERE d.id = :districtId
			  AND s.planType = com.lsit.dfds.enums.PlanType.DAP
			  AND s.financialYear.id = :financialYearId
			  AND s.schemeType.id = :demandCodeId
			""")
	List<Schemes> findDistrictSchemesForDemandCodeDAP(@Param("districtId") Long districtId,
			@Param("financialYearId") Long financialYearId, @Param("demandCodeId") Long demandCodeId);

	Optional<Schemes> findBySchemeCodeAndFinancialYear_Id(String schemeCode, Long id);

}
