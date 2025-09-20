package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.Tax;
import com.lsit.dfds.enums.Statuses;

public interface TaxRepository extends JpaRepository<Tax, Long> {
	List<Tax> findByStatus(Statuses status);

	List<Tax> findByTaxTypeIgnoreCaseAndStatus(String taxType, Statuses status);
}
