package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Schemes;

@Repository
public interface SchemesRepository extends JpaRepository<Schemes, Long> {
	List<Schemes> findBySector_Id(Long sectorId);

	List<Schemes> findByDistricts_Id(Long districtId); // if ManyToMany mapped from both sides

}
