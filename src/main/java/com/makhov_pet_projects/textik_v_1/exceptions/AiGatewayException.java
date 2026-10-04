package com.makhov_pet_projects.textik_v_1.exceptions;

public class AiGatewayException extends RuntimeException {

	public AiGatewayException(String message) {
		super(message);
	}

	public AiGatewayException(String message, Throwable cause) {
		super(message, cause);
	}

}