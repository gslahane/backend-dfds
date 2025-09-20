package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.MLA;
import com.lsit.dfds.enums.Statuses;

@Repository
public interface MLARepository extends JpaRepository<MLA, Long> {

	// Find MLA by its ID. This method returns Optional for null safety.
	Optional<MLA> findMLAById(Long id);

	Optional<MLA> findByUser_Id(Long userId);

	// Find all MLAs
	@Override
	List<MLA> findAll();

	List<MLA> findByConstituency_Id(Long constituencyId);

	List<MLA> findByStatus(Statuses status); // Optional filter

	// ✅ Find MLAs by Constituency → District
	@Query("SELECT m FROM MLA m WHERE m.constituency.district.id = :districtId")
	List<MLA> findByDistrictId(Long districtId);

	List<MLA> findByConstituency_District_Id(Long districtId);

	// import com.lsit.dfds.enums.Statuses;

	@Query("""
			  select m from MLA m
			    join m.constituency c
			    join c.district d
			  where (:districtId is null or d.id = :districtId)
			    and (:constituencyId is null or c.id = :constituencyId)
			    and (:status is null or m.status = :status)
			    and (:search is null or
			         lower(m.mlaName) like lower(concat('%', :search, '%')) or
			         lower(coalesce(m.email,'')) like lower(concat('%', :search, '%')) or
			         coalesce(m.contactNumber,'') like concat('%', :search, '%'))
			  order by m.mlaName asc
			""")
	List<MLA> searchAll(@Param("districtId") Long districtId, @Param("constituencyId") Long constituencyId,
			@Param("status") Statuses status, @Param("search") String search);

	Optional<MLA> findByUserUsername(String username);

	Optional<MLA> findByUserId(Long userId);

	Optional<MLA> findByConstituency_IdAndStatus(Long constituencyId, Statuses status);
}