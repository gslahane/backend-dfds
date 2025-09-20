// src/main/java/com/lsit/dfds/service/StateAccountResolverImpl.java
package com.lsit.dfds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.lsit.dfds.entity.User;
import com.lsit.dfds.entity.UserBankAccount;
import com.lsit.dfds.enums.Roles;
import com.lsit.dfds.enums.Statuses;
import com.lsit.dfds.repo.UserBankAccountRepository;
import com.lsit.dfds.repo.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StateAccountResolverImpl implements StateAccountResolver {

	private final UserRepository userRepo;
	private final UserBankAccountRepository bankRepo;

	@Value("${app.state.debit.vendor:}")
	private String cfgVendorDebit;

	@Value("${app.state.debit.tax:}")
	private String cfgTaxDebit;

	private Optional<String> primaryActive(User u) {
		return bankRepo.findFirstByUser_IdAndIsPrimaryTrueAndStatus(u.getId(), Statuses.ACTIVE)
				.map(UserBankAccount::getAccountNumber);
	}

	private String resolveFromStateUsers() {
		List<User> candidates = new ArrayList<>();
		candidates.addAll(userRepo.findByRole(Roles.STATE_ADMIN));
		candidates.addAll(userRepo.findByRole(Roles.STATE_MAKER));
		candidates.addAll(userRepo.findByRole(Roles.STATE_CHECKER));
		for (User u : candidates) {
			Optional<String> acc = primaryActive(u);
			if (acc.isPresent())
				return acc.get();
		}
		throw new RuntimeException("No active primary State bank account found for STATE_* users");
	}

	@Override
	public String vendorDebitAccount() {
		if (cfgVendorDebit != null && !cfgVendorDebit.isBlank())
			return cfgVendorDebit.trim();
		return resolveFromStateUsers();
	}

	@Override
	public String taxDebitAccount() {
		if (cfgTaxDebit != null && !cfgTaxDebit.isBlank())
			return cfgTaxDebit.trim();
		// fallback to vendor same account
		return vendorDebitAccount();
	}
}
