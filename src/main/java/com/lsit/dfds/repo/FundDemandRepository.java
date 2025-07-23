package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.FundDemand;

@Repository
public interface FundDemandRepository extends JpaRepository<FundDemand, Long> {
	List<FundDemand> findByWork_Id(Long workId);

	List<FundDemand> findByVendor_Id(Long vendorId);

	FundDemand findByDemandId(String demandId);
}
