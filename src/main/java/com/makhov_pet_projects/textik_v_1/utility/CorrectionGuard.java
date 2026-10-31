package com.makhov_pet_projects.textik_v_1.utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CorrectionGuard {

	private static final double MIN_RETAINED_ORIGINAL_TOKENS = 0.5;

	private static final double MAX_LENGTH_RATIO = 1.75;

	private static final int SHORT_MESSAGE_TOKENS = 3;

	public boolean isPlausible(String learnerMessage, String correction) {
		if (learnerMessage == null || correction == null || correction.isBlank()) {
			return true;
		}
		if (correction.strip().equals(learnerMessage.strip())) {
			return false;
		}
		Set<String> originalTokens = tokens(learnerMessage);
		Set<String> correctedTokens = tokens(correction);
		if (originalTokens.isEmpty()) {
			return false;
		}
		if (correctedTokens.isEmpty()) {
			return true;
		}
		if (originalTokens.size() >= SHORT_MESSAGE_TOKENS) {
			long retained = correctedTokens.stream().filter(originalTokens::contains).count();
			if ((double) retained / originalTokens.size() < MIN_RETAINED_ORIGINAL_TOKENS) {
				return false;
			}
		}
		return correctedTokens.size() <= Math.ceil(originalTokens.size() * MAX_LENGTH_RATIO);
	}

	private Set<String> tokens(String text) {
		String normalized = text.toLowerCase(Locale.ROOT)
				.replace('’', '\'')
				.replaceAll("[^a-z0-9']+", " ")
				.replace("'", "")
				.trim();
		if (normalized.isEmpty()) {
			return Set.of();
		}
		List<String> tokens = Arrays.asList(normalized.split("\\s+"));
		return new HashSet<>(tokens);
	}

}
