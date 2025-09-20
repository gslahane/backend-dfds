package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.lsit.dfds.entity.MLC;

public interface MLCRepository extends JpaRepository<MLC, Long> {
	// Find MLA by its ID. This method returns Optional for null safety.
	Optional<MLC> findMLCById(Long id);

	Optional<MLC> findByUser_Id(Long userId);

	// Find all MLAs
	@Override
	List<MLC> findAll();

	// ✅ Fetch MLCs by a given district (nodal or accessible district)
	@Query("SELECT m FROM MLC m JOIN m.accessibleDistricts d WHERE d.id = :districtId")
	List<MLC> findByAccessibleDistrict(Long districtId);

	// ✅ Fetch MLCs that belong to their **own nodal district**
	@Query("SELECT m FROM MLC m WHERE  m.district.id = :districtId")
	List<MLC> findByNodalDistrict(Long districtId);

	// ✅ Fetch all MLCs by districtId (nodal or accessible)
	@Query("SELECT DISTINCT m FROM MLC m LEFT JOIN m.accessibleDistricts ad WHERE m.district.id = :districtId OR ad.id = :districtId")
	List<MLC> findAllByDistrictId(Long districtId);

}