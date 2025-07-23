package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.DistrictSchemeAllocation;

@Repository
public interface DistrictSchemeAllocationRepository extends JpaRepository<DistrictSchemeAllocation, Long> {
	List<DistrictSchemeAllocation> findByDistrict_Id(Long districtId);

	List<DistrictSchemeAllocation> findByScheme_Id(Long schemeId);

	DistrictSchemeAllocation findByDistrict_IdAndScheme_Id(Long districtId, Long schemeId);
}
