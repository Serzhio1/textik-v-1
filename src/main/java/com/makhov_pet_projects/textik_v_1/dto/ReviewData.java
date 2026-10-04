package com.makhov_pet_projects.textik_v_1.dto;

import java.util.List;

public record ReviewData(
		String textSummary,
		String conversationSummary,
		String mistakes,
		String conversationGrade,
		List<String> mainErrors) {
}