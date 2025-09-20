// src/main/java/com/lsit/dfds/dto/PaymentFileResult.java
package com.lsit.dfds.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentFileResult {
	private String fileName;
	private String absolutePath;
	private int totalRows;
	private double totalAmount;
	private List<Long> demandIds;
}
