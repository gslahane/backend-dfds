// WorkStatusCountDto.java
package com.lsit.dfds.dto;

import com.lsit.dfds.enums.WorkStatuses;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkStatusCountDto {
	private WorkStatuses status;
	private Long count;
}
