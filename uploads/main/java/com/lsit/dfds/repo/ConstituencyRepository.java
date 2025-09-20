package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.Constituency;

public interface ConstituencyRepository extends JpaRepository<Constituency, Long> {

	List<Constituency> findByDistrict_Id(Long districtId);

}
