package com.lsit.dfds.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.DistrictLimitAllocation;

@Repository
public interface DistrictLimitAllocationRepository extends JpaRepository<DistrictLimitAllocation, Long> {
	DistrictLimitAllocation findByDistrict_Id(Long districtId);
}
