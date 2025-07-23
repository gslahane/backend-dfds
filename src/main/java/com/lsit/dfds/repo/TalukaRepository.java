package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.Taluka;

public interface TalukaRepository extends JpaRepository<Taluka, Long> {

	List<Taluka> findByDistrict_Id(Long districtId);

}
