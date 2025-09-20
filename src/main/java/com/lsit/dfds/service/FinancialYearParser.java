package com.lsit.dfds.service;

import org.springframework.stereotype.Component;

import com.lsit.dfds.entity.FinancialYear;

@Component
public class FinancialYearParser {
	public int startYearOf(FinancialYear fy) {
		if (fy.getStartYear() != null) {
			return fy.getStartYear();
		}
		String s = fy.getFinacialYear();
		if (s != null) {
			var m = java.util.regex.Pattern.compile("^(\\d{4})").matcher(s.trim());
			if (m.find()) {
				return Integer.parseInt(m.group(1));
			}
		}
		throw new RuntimeException("Cannot resolve startYear for FY: " + s);
	}
}
