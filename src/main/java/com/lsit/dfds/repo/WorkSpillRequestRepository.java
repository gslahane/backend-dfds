package com.lsit.dfds.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.WorkSpillRequest;
import com.lsit.dfds.enums.SpillStatuses;

public interface WorkSpillRequestRepository extends JpaRepository<WorkSpillRequest, Long> {
	List<WorkSpillRequest> findByStatus(SpillStatuses status);

	List<WorkSpillRequest> findByDistrict_Id(Long districtId);

	List<WorkSpillRequest> findByToFy_Id(Long fyId);

	List<WorkSpillRequest> findByDemandCode_Id(Long demandCodeId);

	List<WorkSpillRequest> findByWork_Id(Long workId);
}
