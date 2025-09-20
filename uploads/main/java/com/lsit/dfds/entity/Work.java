package com.lsit.dfds.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lsit.dfds.enums.WorkStatuses;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "lsit_dfds_work")
public class Work {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = true)
	private String workName;

	@Column(nullable = true)
	private String workCode;

	private Double adminApprovedAmount = 0.0;

	@Column(nullable = true)
	private String adminApprovedletterUrl;

	private Double workPortionTax = 0.0;

	private Double workPortionAmount = 0.0;

	@Column(nullable = true)
	private String workOrderLetterUrl;

	// ✅ Proper relation to FinancialYear (FK: financial_year_id)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "financial_year_id", nullable = true)
	@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
	private FinancialYear financialYear;

	@Column(nullable = true)
	private LocalDate workStartDate;

	@Column(nullable = true)
	private LocalDate workEndDate;

	@Column(nullable = true)
	private LocalDate sanctionDate;

	private Double grossAmount = 0.0;

	private Double balanceAmount = 0.0;

	@Column(nullable = true)
	private String description;

	@Column(nullable = true)
	private String remark;

	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private WorkStatuses status;

	// ✅ New: URL to stored recommendation letter (uploaded during recommendation)
	@Column(nullable = true)
	private String recommendationLetterUrl;

	@CreationTimestamp
	private LocalDateTime createdAt;

	@Column(nullable = true)
	private String createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "scheme_id")
	private Schemes scheme;

	@OneToOne
	@JoinColumn(name = "vendor_id", referencedColumnName = "id")
	private Vendor vendor; // Already Present

	@ElementCollection
	@CollectionTable(name = "lsit_dfds_work_taxes", joinColumns = @JoinColumn(name = "work_id"))
	private List<TaxDetail> appliedTaxes; // Store selected taxes for the work

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "constituency_id")
	private Constituency constituency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mla_id")
	private MLA recommendedByMla;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "mlc_id")
	private MLC recommendedByMlc;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "district_id", nullable = false)
	private District district;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "hadp_id", nullable = true)
	private HADP hadp;

	@Column(nullable = true)
	private Boolean isNodalWork; // true = nodal, false = non-nodal

	// Work.java (add inside the entity)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ia_user_id", nullable = true)
	private User implementingAgency; // IA assigned by district approvals

}
