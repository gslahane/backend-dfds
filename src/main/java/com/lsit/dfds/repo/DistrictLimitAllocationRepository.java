package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.DemandCode;
import com.lsit.dfds.entity.District;
import com.lsit.dfds.entity.DistrictLimitAllocation;
import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.enums.PlanType;

@Repository
public interface DistrictLimitAllocationRepository extends JpaRepository<DistrictLimitAllocation, Long> {

	DistrictLimitAllocation findByDistrict_Id(Long districtId);

	Optional<District> findDistrictById(Long districtId);

	Optional<FinancialYear> findFinancialYearById(Long financialYearId);

	Optional<DemandCode> findSchemeTypeById(Long schemeTypeId);

	DistrictLimitAllocation findFirstByDistrict_IdAndFinancialYear_Id(Long districtId, Long financialYearId);

	List<DistrictLimitAllocation> findAllByDistrict_IdAndFinancialYear_Id(Long districtId, Long financialYearId);

	// List<DistrictLimitAllocation> findByDistrict_IdAndScheme_PlanType(Long
	// districtId, PlanType planType);

	Optional<DistrictLimitAllocation> findByDistrict_IdAndFinancialYear_IdAndPlanType(Long districtId,
			Long financialYearId, PlanType planType);

	Optional<DistrictLimitAllocation> findByDistrict_IdAndFinancialYear_IdAndPlanTypeAndSchemeType_Id(Long districtId,
			Long financialYearId, PlanType planType, Long schemeTypeId);

	List<DistrictLimitAllocation> findAllByFinancialYear_IdAndPlanType(Long financialYearId, PlanType dap);

	/*
	 * Optional<User>
	 * findByDistrict_IdAndFinancialYear_IdAndPlanTypeAndSchemeType_Id(Long id, Long
	 * id2, PlanType dap, Long id3);
	 */

}
