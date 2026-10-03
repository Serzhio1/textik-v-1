package com.makhov_pet_projects.textik_v_1.utility;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Interests {

	public List<String> split(String raw) {
		if (raw == null || raw.isBlank()) {
			return List.of();
		}
		return Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(interest -> !interest.isEmpty())
				.toList();
	}

	public String normalize(String raw) {
		return String.join(", ", split(raw));
	}

}
