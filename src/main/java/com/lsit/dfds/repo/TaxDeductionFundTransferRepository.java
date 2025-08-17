package com.lsit.dfds.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.lsit.dfds.entity.TaxDeductionFundTransfer;

@Repository
public interface TaxDeductionFundTransferRepository extends JpaRepository<TaxDeductionFundTransfer, Long> {
//	TaxDeductionFundTransfer findByFundDemand_Id(Long fundDemandId);
	Optional<TaxDeductionFundTransfer> findByFundDemand_Id(Long fundDemandId);

}
