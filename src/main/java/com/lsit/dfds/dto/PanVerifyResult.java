package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PanVerifyResult {
	private boolean ok;
	private String code;
	private String message;
	private Object data;
}
