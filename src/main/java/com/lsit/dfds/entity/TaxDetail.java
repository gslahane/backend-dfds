package com.lsit.dfds.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class TaxDetail {

	private String taxName;

	private Double taxPercentage = 0.0;

	private Double taxAmount = 0.0;
}
