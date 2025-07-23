package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Work;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {
	List<Work> findByScheme_Id(Long schemeId);

	List<Work> findByVendor_Id(Long vendorId);
}
