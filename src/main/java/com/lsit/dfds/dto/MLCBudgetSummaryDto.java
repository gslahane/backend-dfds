package com.lsit.dfds.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MLCBudgetSummaryDto {
	private String financialYear; // e.g. "2024-25"
	private String accountName; // MLA/MLC Name or Account Name
	private Double allocatedBudget; // Total allocated
	private Double utilizedAmount; // Utilized from the budget
	private Double balanceLimit; // Remaining = allocated - utilized
}
