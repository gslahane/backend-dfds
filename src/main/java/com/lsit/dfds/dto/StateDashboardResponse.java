// src/main/java/com/lsit/dfds/dto/StateDashboardResponse.java
package com.lsit.dfds.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class StateDashboardResponse {

	private String planType; // ML-MLA | ML-MLC | HADP
	private Long financialYearId; // nullable == All
	private Long districtId; // optional filter
	private String search; // optional
	private Boolean showPendingOnly; // optional
	private int page; // 1-based
	private int size;

	// overview (for top-left small box)
	private Double totalMlaFund = 0.0;
	private Double totalMlcFund = 0.0;
	private Double totalHadpFund = 0.0;

	// cards (dynamic per planType)
	// for ML-MLA: totalBudget, disbursed, balance, pending (amount + count)
	// for ML-MLC: same idea
	// for HADP : total, approved, balance, pending
	private Map<String, Object> cards;

	// paginated table
	private long totalElements;
	private int totalPages;

	private List<MlaRow> mlaRows; // only filled for planType=ML-MLA
	private List<MlcRow> mlcRows; // only for planType=ML-MLC
	private List<HadpRow> hadpRows;// only for planType=HADP

	private RowFooter footer; // totals of the filtered set

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class RowFooter {
		private Double budget; // total budget in filtered rows
		private Double fundUtilized; // total utilized
		private Double balance; // total balance
		private Double pendingAmount; // total pending demand amount
		private Long pendingCount; // total pending demand count
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class MlaRow {
		private Long mlaId;
		private String mlaName;
		private Long districtId;
		private String districtName;
		private String constituencyName;
		private Double budget;
		private Double fundUtilized;
		private Double balance;
		private Double pendingAmount; // sum of PENDING + APPROVED_BY_STATE
		private Long pendingCount;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class MlcRow {
		private Long mlcId;
		private String mlcName;
		private Long nodalDistrictId;
		private String nodalDistrictName;
		private Double budget;
		private Double fundUtilized;
		private Double balance;
		private Double pendingAmount;
		private Long pendingCount;
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class HadpRow {
		private Long talukaId; // null for district aggregate row (if you choose)
		private String talukaName;
		private Long districtId;
		private String districtName;

		private String demandCode; // optional grouping key (frontend shows this)
		private Double budget; // allocated/aggregated
		private Double fundUtilized; // approved/transferred
		private Double balance;
		private Double pendingAmount;
		private Long pendingCount;
	}
}
