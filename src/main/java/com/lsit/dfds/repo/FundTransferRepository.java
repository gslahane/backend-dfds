package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.FundTransfer;

public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {
	Optional<FundTransfer> findByFundDemand_Id(Long fundDemandId);

	@Query("""
			  select ft from FundTransfer ft
			  where ft.fundDemand.vendor.id = :vendorId
			    and (:financialYearId is null or ft.fundDemand.financialYear.id = :financialYearId)
			""")
	List<FundTransfer> findTransfersForVendor(@Param("vendorId") Long vendorId,
			@Param("financialYearId") Long financialYearId);
}
