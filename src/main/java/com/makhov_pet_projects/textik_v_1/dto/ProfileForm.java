package com.makhov_pet_projects.textik_v_1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProfileForm(
		@NotBlank(message = "Tell us about yourself")
		@Size(max = 2000, message = "Text is too long — maximum 2000 characters")
		String about,

		@NotBlank(message = "Enter your interests")
		@Size(max = 500, message = "List is too long — maximum 500 characters")
		String interests,

		@NotBlank(message = "Choose a level")
		@Pattern(regexp = LEVELS_PATTERN, message = "Choose level A1, A2 or B1")
		String level) {

	public static final List<String> LEVELS = List.of("A1", "A2", "B1");

	private static final String LEVELS_PATTERN = "A1|A2|B1";

	public static ProfileForm empty() {
		return new ProfileForm("", "", "");
	}

}
