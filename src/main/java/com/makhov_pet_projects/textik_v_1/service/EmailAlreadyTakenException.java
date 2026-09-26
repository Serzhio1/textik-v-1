package com.makhov_pet_projects.textik_v_1.service;

public class EmailAlreadyTakenException extends RuntimeException {

	public EmailAlreadyTakenException(String email) {
		super("Email already taken: " + email);
	}

}
