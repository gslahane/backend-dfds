package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.FinancialYearBudget;
import com.lsit.dfds.enums.PlanType;

public interface FinancialYearBudgetRepository extends JpaRepository<FinancialYearBudget, Long> {
	// Find all FinancialYearBudget records for a given financialYearId
	List<FinancialYearBudget> findByFinancialYear_Id(Long financialYearId);

	FinancialYearBudget findByPlanTypeAndFinancialYear_Id(PlanType planType, Long financialYearId);

	Optional<FinancialYearBudget> findByFinancialYear_IdAndPlanType(Long id, PlanType dap);
}