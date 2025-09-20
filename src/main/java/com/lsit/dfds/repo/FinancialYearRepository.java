package com.lsit.dfds.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.FinancialYear;
import com.lsit.dfds.entity.User;

public interface FinancialYearRepository extends JpaRepository<FinancialYear, Long> {

	Optional<FinancialYear> findFinancialYearById(Long financialYearId);

	Optional<User> findByFinacialYearIgnoreCase(String v);

}
