package com.makhov_pet_projects.textik_v_1.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ChatCompletionIds {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	public String ensureId(String json) {
		return ensureId(json, "chatcmpl-" + UUID.randomUUID());
	}

	String ensureId(String json, String id) {
		try {
			JsonNode root = MAPPER.readTree(json);
			if (!(root instanceof ObjectNode object) || !object.has("choices") || object.has("id")) {
				return json;
			}
			object.put("id", id);
			return MAPPER.writeValueAsString(object);
		} catch (Exception e) {
			return json;
		}
	}

}