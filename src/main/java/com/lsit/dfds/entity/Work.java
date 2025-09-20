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
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    /** NEW: Amount recommended by MLA/MLC/HADP */
    @Column(nullable = true)
    private Double recommendedAmount = 0.0;

    /** NEW: Amount approved at district workflow (ADPO/DPO/Collector) */
    @Column(nullable = true)
    private Double districtApprovedAmount = 0.0;

    /** AA (final sanction) */
    private Double adminApprovedAmount = 0.0;

    @Column(nullable = true)
    private String adminApprovedletterUrl;

    private Double workPortionTax = 0.0;
    private Double workPortionAmount = 0.0;

    @Column(nullable = true)
    private String workOrderLetterUrl;

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

    /** IA-estimated gross amount (set later by IA) */
    private Double grossAmount = 0.0;

    /** Budget balance (moves at AA and during demands) */
    private Double balanceAmount = 0.0;

    @Column(nullable = true)
    private String description;

    @Column(nullable = true)
    private String remark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private WorkStatuses status;

    @Column(name = "recommendation_letter_url", nullable = true, length = 512)
    private String recommendationLetterUrl;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private String createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheme_id")
    private Schemes scheme;

	/** Many works can be assigned to one vendor */
	@ManyToOne(fetch = FetchType.EAGER, optional = true)
	@JoinColumn(name = "vendor_id", foreignKey = @ForeignKey(name = "fk_work_vendor"))
	private Vendor vendor;



    @ElementCollection
    @CollectionTable(name = "lsit_dfds_work_taxes", joinColumns = @JoinColumn(name = "work_id"))
    private List<TaxDetail> appliedTaxes;

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
    private Boolean isNodalWork;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ia_user_id", nullable = true)
    private User implementingAgency;
}
