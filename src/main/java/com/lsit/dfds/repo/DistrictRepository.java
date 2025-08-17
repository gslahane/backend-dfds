package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.District;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {

	List<District> findByDivision_Id(Long divisionId);

	Optional<District> findByDistrictNameIgnoreCase(String districtName);
}
