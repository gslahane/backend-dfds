package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericResponseDto<T> {
	private boolean success;
	private String message;
	private T data;
}
