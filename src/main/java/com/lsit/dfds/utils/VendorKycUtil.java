package com.lsit.dfds.utils;

import org.springframework.stereotype.Component;

import com.lsit.dfds.dto.PanVerifyResult;
import com.lsit.dfds.entity.Vendor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VendorKycUtil {

	private final PanKycClient panKycClient;

	/** Verify PAN “now”. Used in /pan/verify endpoint, not in registration. */
	public PanVerifyResult verifyPanNow(String pan) {
		if (pan == null || pan.isBlank())
			throw new RuntimeException("PAN required");
		return panKycClient.verifyPan(pan.trim().toUpperCase());
	}

	/** On update: re-verify only if PAN actually changed. */
	public void verifyOnUpdateIfPanChanged(Vendor existing, Vendor updating) {
		String oldPan = ns(existing.getPanNumber());
		String newPan = ns(updating.getPanNumber());
		if (!oldPan.equals(newPan)) {
			updating.setPanVerified(false);
			if (!newPan.isBlank()) {
				PanVerifyResult pr = panKycClient.verifyPan(newPan);
				if (!pr.isOk())
					throw new RuntimeException("PAN verification failed: " + pr.getCode() + " - " + pr.getMessage());
				updating.setPanVerified(true);
			}
		} else {
			updating.setPanVerified(existing.getPanVerified());
		}
	}

	private static String ns(String s) {
		return s == null ? "" : s.trim().toUpperCase();
	}
}
