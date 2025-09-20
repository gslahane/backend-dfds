package com.lsit.dfds.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MlcListFilterDto {
	private Long districtId; // optional; auto-binds for district logins in service
	private String status; // ACTIVE/INACTIVE/CLOSED (string -> enum mapping in service)
	private String search; // name/email/phone (contains)
}
