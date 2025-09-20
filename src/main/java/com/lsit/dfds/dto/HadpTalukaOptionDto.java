// src/main/java/com/lsit/dfds/dto/HadpTalukaOptionDto.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HadpTalukaOptionDto {
	private Long id; // talukaId
	private String name; // talukaName
	private String hadpType; // GROUP / SUB_GROUP (nullable)
}
