// src/main/java/com/lsit/dfds/service/StateAccountResolver.java
package com.lsit.dfds.service;

public interface StateAccountResolver {
	/** State debit used for vendor payouts */
	String vendorDebitAccount();

	/** State debit used for tax transfers (may be same as vendor) */
	String taxDebitAccount();
}
