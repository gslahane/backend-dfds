package com.lsit.dfds.repo;

import org.springframework.data.jpa.domain.Specification;

import com.lsit.dfds.entity.Work;
import com.lsit.dfds.enums.PlanType;
import com.lsit.dfds.enums.WorkStatuses;

public class WorkSpecs {

	public static Specification<Work> statusIs(WorkStatuses status) {
		return (root, q, cb) -> cb.equal(root.get("status"), status);
	}

	public static Specification<Work> planTypeIs(PlanType planType) {
		return (root, q, cb) -> planType == null ? cb.conjunction()
				: cb.equal(root.join("scheme").get("planType"), planType);
	}

	public static Specification<Work> fyIdIs(Long fyId) {
		return (root, q, cb) -> fyId == null ? cb.conjunction() : cb.equal(root.join("financialYear").get("id"), fyId);
	}

	public static Specification<Work> schemeIdIs(Long schemeId) {
		return (root, q, cb) -> schemeId == null ? cb.conjunction() : cb.equal(root.join("scheme").get("id"), schemeId);
	}

	public static Specification<Work> districtIdIs(Long districtId) {
		return (root, q, cb) -> districtId == null ? cb.conjunction()
				: cb.equal(root.join("district").get("id"), districtId);
	}

	public static Specification<Work> iaIs(Long iaUserId) {
		return (root, q, cb) -> iaUserId == null ? cb.conjunction()
				: cb.equal(root.join("implementingAgency").get("id"), iaUserId);
	}
}
