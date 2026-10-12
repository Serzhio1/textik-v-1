package com.makhov_pet_projects.textik_v_1.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationForm(
		@NotBlank(message = "Enter your email")
		@Email(message = "Invalid email address")
		@Size(max = 255, message = "Email is too long")
		String email,

		@NotBlank(message = "Enter your name")
		@Size(max = 255, message = "Name is too long")
		String username,

		@NotBlank(message = "Enter a password")
		@Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
		String password,

		@NotBlank(message = "Repeat your password")
		String passwordConfirm) {

	public static RegistrationForm empty() {
		return new RegistrationForm("", "", "", "");
	}

	@AssertTrue(message = "Passwords do not match")
	public boolean isPasswordConfirmed() {
		return password == null || password.isEmpty() || password.equals(passwordConfirm);
	}

}
