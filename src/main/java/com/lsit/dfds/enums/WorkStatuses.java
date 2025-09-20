// com.lsit.dfds.enums.WorkStatuses
package com.lsit.dfds.enums;

public enum WorkStatuses {
	// existing
	APPROVED, REJECTED, CANCELLED, PENDING,

	// new workflow stages (safe: EnumType.STRING everywhere)
	PROPOSED, // MLA submitted
	ADPO_APPROVED, ADPO_REJECTED, DPO_APPROVED, DPO_REJECTED, COLLECTOR_APPROVED, COLLECTOR_REJECTED, AA_APPROVED, // Admin
																													// Approval
																													// finalized
																													// (AA
																													// amount
																													// set
																													// +
																													// sanctionDate
	// set)
	WORK_ORDER_ISSUED, // IA issued work order (vendor mapped)
	IN_PROGRESS, COMPLETED
}
