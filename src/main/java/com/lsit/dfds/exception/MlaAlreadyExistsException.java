package com.lsit.dfds.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class MlaAlreadyExistsException extends ResponseStatusException {
	public MlaAlreadyExistsException(String message) {
		super(HttpStatus.CONFLICT, message);
	}
}
