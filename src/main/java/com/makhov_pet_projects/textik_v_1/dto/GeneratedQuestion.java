package com.makhov_pet_projects.textik_v_1.dto;

import java.util.List;

public record GeneratedQuestion(
		String question,
		List<String> options,
		String correctAnswer,
		String explanation) {
}