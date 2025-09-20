// src/main/java/com/lsit/dfds/dto/FundDemandStatusUpdateResult.java
package com.lsit.dfds.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class FundDemandStatusUpdateResult {
	private int updated;
	private List<Long> updatedIds = new ArrayList<>();
	private List<String> errors = new ArrayList<>();
}
