package com.lsit.dfds.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.Vendor;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

	Long countByCreatedBy(String createdBy);

}
