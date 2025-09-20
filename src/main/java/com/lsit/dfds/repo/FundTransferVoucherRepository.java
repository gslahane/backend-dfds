// src/main/java/com/lsit/dfds/repo/FundTransferVoucherRepository.java
package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.lsit.dfds.entity.FundTransferVoucher;

public interface FundTransferVoucherRepository extends JpaRepository<FundTransferVoucher, Long> {
	Optional<FundTransferVoucher> findByFundDemand_Id(Long fundDemandId);

	Optional<FundTransferVoucher> findByVoucherNo(String voucherNo);

	@Query("""
			  SELECT v FROM FundTransferVoucher v
			  JOIN v.fundDemand d
			  LEFT JOIN d.financialYear fy
			  LEFT JOIN d.work w
			  LEFT JOIN w.district dist
			  WHERE (:financialYearId IS NULL OR fy.id = :financialYearId)
			    AND (:districtId     IS NULL OR dist.id = :districtId)
			    AND (:createdBy      IS NULL OR d.createdBy = :createdBy)
			    AND (:status         IS NULL OR d.status = :status)
			    AND (
			         :q IS NULL
			         OR LOWER(d.demandId) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(v.vendorName) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.workName) LIKE LOWER(CONCAT('%', :q, '%'))
			         OR LOWER(w.workCode) LIKE LOWER(CONCAT('%', :q, '%'))
			    )
			  ORDER BY v.createdAt DESC
			""")
	List<FundTransferVoucher> findFiltered(@Param("financialYearId") Long financialYearId,
			@Param("districtId") Long districtId, @Param("createdBy") String createdBy,
			@Param("status") com.lsit.dfds.enums.DemandStatuses status, @Param("q") String q);

//	Optional<FundTransferVoucher> findByFundDemand_Id(Long fundDemandId);

}
