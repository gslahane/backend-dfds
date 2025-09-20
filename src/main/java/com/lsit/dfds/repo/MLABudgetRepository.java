package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.MLABudget;

@Repository
public interface MLABudgetRepository extends JpaRepository<MLABudget, Long> {

//	List<MLABudget> findByFinancialYear_Id(Long financialYearId);

	List<MLABudget> findByMla_IdAndFinancialYear_Id(Long mlaId, Long financialYearId);

	List<MLABudget> findByScheme_IdAndFinancialYear_Id(Long schemeId, Long financialYearId);

	MLABudget findByMlaIdAndFinancialYearId(Long financialYearId, Long mlaId);

//	Optional<FinancialYear> findByMla_IdAndFinancialYear_IdAndConstituency_Id(Long mlaId, Long financialYearId,
//			Long constituencyId);

	Optional<MLABudget> findByMla_IdAndFinancialYear_IdAndConstituency_Id(Long mlaId, Long financialYearId,
			Long constituencyId);

	List<MLABudget> findByFinancialYear_Id(Long financialYearId);

	Optional<MLABudget> findByMla_IdAndScheme_IdAndFinancialYear_Id(Long id, Long id2, Long id3);

	List<MLABudget> findByMla_Id(Long mlaId);

	@Query("""
			  select b from MLABudget b
			  where b.mla.constituency.district.id = :districtId
			    and (:fyId is null or b.financialYear.id = :fyId)
			""")
	List<MLABudget> findByDistrictAndFy(@Param("districtId") Long districtId, @Param("fyId") Long fyId);

	@Query("""
			  select coalesce(sum(b.allocatedLimit),0) from MLABudget b
			  where b.mla.constituency.district.id = :districtId
			    and (:fyId is null or b.financialYear.id = :fyId)
			""")
	Double sumAllocatedByDistrictAndFy(@Param("districtId") Long districtId, @Param("fyId") Long fyId);

	@Query("""
			  select b from MLABudget b
			  where b.mla.id = :mlaId and (:fyId is null or b.financialYear.id = :fyId)
			""")
	List<MLABudget> findByMlaAndFy(@Param("mlaId") Long mlaId, @Param("fyId") Long fyId);

	List<MLABudget> findByConstituency_District_Id(Long districtId);

	List<MLABudget> findByFinancialYear_IdAndConstituency_District_Id(Long financialYearId, Long districtId);

	@Query("select coalesce(sum(b.allocatedLimit),0) from MLABudget b")
	Double sumAllocatedAllFys();

	@Query("select coalesce(sum(b.allocatedLimit),0) from MLABudget b where b.financialYear.id = :financialYearId")
	Double sumAllocatedByFy(Long financialYearId);

	@Query("select b from MLABudget b where b.status = com.lsit.dfds.enums.Statuses.ACTIVE")
	List<MLABudget> findAllActive();

	@Query("""
			select coalesce(sum(b.allocatedLimit),0)
			from MLABudget b
			where (:fyId is null or b.financialYear.id = :fyId)
			""")
	Double sumAllocatedByFyNullable(Long fyId);

}
