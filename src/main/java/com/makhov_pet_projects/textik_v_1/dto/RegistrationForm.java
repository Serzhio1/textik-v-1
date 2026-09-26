package com.makhov_pet_projects.textik_v_1.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationForm(
		@NotBlank(message = "Укажите email")
		@Email(message = "Некорректный email")
		@Size(max = 255, message = "Слишком длинный email")
		String email,

		@NotBlank(message = "Укажите имя")
		@Size(max = 255, message = "Слишком длинное имя")
		String username,

		@NotBlank(message = "Укажите пароль")
		@Size(min = 8, max = 72, message = "Пароль должен быть от 8 до 72 символов")
		String password,

		@NotBlank(message = "Повторите пароль")
		String passwordConfirm) {

	public static RegistrationForm empty() {
		return new RegistrationForm("", "", "", "");
	}

	@AssertTrue(message = "Пароли не совпадают")
	public boolean isPasswordConfirmed() {
		return password == null || password.isEmpty() || password.equals(passwordConfirm);
	}

}
