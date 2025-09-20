package com.lsit.dfds.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.enums.Statuses;

public interface UserBankAccountRepository extends JpaRepository<UserBankAccount, Long> {
	Optional<UserBankAccount> findFirstByUser_IdAndIsPrimaryTrueAndStatus(Long userId, Statuses status);

	List<UserBankAccount> findByUser_IdAndStatus(Long userId, Statuses status);

	List<UserBankAccount> findByUserId(Long id);
}
