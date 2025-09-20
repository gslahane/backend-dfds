package com.lsit.dfds.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserBankAccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankAccountService {

	private final UserBankAccountRepository repo;

	public Optional<UserBankAccount> getPrimaryForUser(Long userId) {
		return repo.findFirstByUser_IdAndIsPrimaryTrueAndStatus(userId, Statuses.ACTIVE);
	}

	public List<UserBankAccount> getActiveAccountsForUser(Long userId) {
		return repo.findByUser_IdAndStatus(userId, Statuses.ACTIVE);
	}

	/**
	 * Sets the given account as primary. Demotes any existing primary for that
	 * user. The provided 'acc' must have acc.setUser(user) already OR will be
	 * inferred from existing accounts.
	 */
	@Transactional
	public UserBankAccount setPrimary(Long userId, UserBankAccount acc, String actor) {
		var actives = repo.findByUser_IdAndStatus(userId, Statuses.ACTIVE);
		// Ensure we carry the user reference if not set
		if (acc.getUser() == null && !actives.isEmpty()) {
			acc.setUser(actives.get(0).getUser());
		}
		// Demote existing primaries
		actives.forEach(a -> {
			if (Boolean.TRUE.equals(a.getIsPrimary())) {
				a.setIsPrimary(false);
			}
		});

		acc.setIsPrimary(true);
		acc.setStatus(Statuses.ACTIVE);
		acc.setCreatedBy(actor);

		return repo.save(acc);
	}
}
