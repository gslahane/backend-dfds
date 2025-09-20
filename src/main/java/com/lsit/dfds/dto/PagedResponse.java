// src/main/java/com/lsit/dfds/dto/PagedResponse.java
package com.lsit.dfds.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PagedResponse<T> {
	private List<T> content;
	private long totalElements;
	private int totalPages;
	private int page;
	private int size;
}
