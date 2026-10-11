package com.makhov_pet_projects.textik_v_1.utility;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ChatCompletionJson {

	private static final ObjectMapper MAPPER = new ObjectMapper(
			JsonFactory.builder()
					.enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
					.build());

	private static final Pattern FENCED = Pattern.compile("```(?:[a-zA-Z]*)?\\s*(.*?)```", Pattern.DOTALL);

	public String sanitize(String answer) {
		Matcher fence = FENCED.matcher(answer);
		if (fence.find()) {
			String cleaned = clean(fence.group(1));
			if (cleaned != null) {
				return cleaned;
			}
		}
		String trimmed = answer.strip();
		if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
			String cleaned = clean(trimmed);
			if (cleaned != null) {
				return cleaned;
			}
		}
		return answer;
	}

	private String clean(String json) {
		try {
			JsonNode node = MAPPER.readTree(json);
			if (node.isObject() || node.isArray()) {
				return MAPPER.writeValueAsString(node);
			}
			return null;
		} catch (Exception e) {
			return null;
		}
	}

}