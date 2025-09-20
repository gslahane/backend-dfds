package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.MLCBudget;

public interface MLCBudgetRepository extends JpaRepository<MLCBudget, Long> {
	// You can define custom query methods here if needed
	MLCBudget findByMlcIdAndFinancialYearId(Long MLCId, Long financialYearId);

	// Example to find all MLC budgets by MLA ID
	List<MLCBudget> findByMlcId(Long MLCId);

	List<MLCBudget> findByFinancialYear_Id(Long financialYearId);

	Optional<MLCBudget> findByMlc_IdAndScheme_IdAndFinancialYear_Id(Long id, Long id2, Long id3);

	List<MLCBudget> findByMlc_Id(Long mlcId);

	List<MLCBudget> findByMlc_IdAndFinancialYear_Id(Long mlcId, Long financialYearId);

	Optional<MLCBudget> findByMlc_IdAndFinancialYear_IdAndScheme_Id(Long id, Long id2, Long id3);

//	Optional<MLCBudget> findByMlc_IdAndFinancialYear_Id(Long mlcId, Long fyId);

	// You can add more methods depending on your needs.

	@Query("""
			  select b from MLCBudget b
			  where (:mlcId is null or b.mlc.id = :mlcId)
			    and (:fyId is null or b.financialYear.id = :fyId)
			""")
	List<MLCBudget> findByMlcAndFy(@Param("mlcId") Long mlcId, @Param("fyId") Long fyId);

	@Query("""
			  select coalesce(sum(b.allocatedLimit),0) from MLCBudget b
			  where (:fyId is null or b.financialYear.id = :fyId)
			    and b.mlc.id in :mlcIds
			""")
	Double sumAllocatedForMlcs(@Param("mlcIds") List<Long> mlcIds, @Param("fyId") Long fyId);

	// ✅ correct property path: MLCBudget -> mlc -> district -> id
	List<MLCBudget> findByMlc_District_Id(Long districtId);

	List<MLCBudget> findByFinancialYear_IdAndMlc_District_Id(Long financialYearId, Long districtId);

	@Query("select coalesce(sum(b.allocatedLimit),0) from MLCBudget b")
	Double sumAllocatedAllFys();

	@Query("select coalesce(sum(b.allocatedLimit),0) from MLCBudget b where b.financialYear.id = :financialYearId")
	Double sumAllocatedByFy(Long financialYearId);

	@Query("select b from MLCBudget b where b.status = com.lsit.dfds.enums.Statuses.ACTIVE")
	List<MLCBudget> findAllActive();

	@Query("""
			select coalesce(sum(b.allocatedLimit),0)
			from MLCBudget b
			where (:fyId is null or b.financialYear.id = :fyId)
			""")
	Double sumAllocatedByFyNullable(Long fyId);
}