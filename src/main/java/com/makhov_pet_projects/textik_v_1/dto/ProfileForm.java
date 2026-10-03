package com.makhov_pet_projects.textik_v_1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileForm(
		@NotBlank(message = "Расскажите о себе")
		@Size(max = 2000, message = "Слишком длинный текст — максимум 2000 символов")
		String about,

		@NotBlank(message = "Укажите увлечения")
		@Size(max = 500, message = "Слишком длинный список — максимум 500 символов")
		String interests,

		@NotBlank(message = "Выберите уровень")
		@Pattern(regexp = LEVELS_PATTERN, message = "Выберите уровень A1, A2 или B1")
		String level) {

	public static final List<String> LEVELS = List.of("A1", "A2", "B1");

	private static final String LEVELS_PATTERN = "A1|A2|B1";

	public static ProfileForm empty() {
		return new ProfileForm("", "", "");
	}

}
