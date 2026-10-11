package com.makhov_pet_projects.textik_v_1.utility;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Initials {

	public String of(String displayName) {
		List<String> words = Arrays.stream(displayName == null ? new String[0] : displayName.trim().split("\\s+"))
				.filter(word -> !word.isEmpty())
				.toList();
		if (words.isEmpty()) {
			return "?";
		}
		String first = words.get(0).substring(0, 1);
		if (words.size() == 1) {
			return first.toUpperCase(Locale.ROOT);
		}
		String second = words.get(1).substring(0, 1);
		return (first + second).toUpperCase(Locale.ROOT);
	}

}
