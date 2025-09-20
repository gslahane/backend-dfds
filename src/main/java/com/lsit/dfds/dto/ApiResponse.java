// ApiResponse.java
package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {
	private boolean success;
	private String message;
	private T data;

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, "OK", data);
	}

	public static <T> ApiResponse<T> error(String msg) {
		return new ApiResponse<>(false, msg, null);
	}
}
