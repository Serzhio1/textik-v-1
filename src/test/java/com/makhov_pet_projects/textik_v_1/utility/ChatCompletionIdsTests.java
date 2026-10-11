package com.makhov_pet_projects.textik_v_1.utility;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Добавление id в ответ GigaChat")
class ChatCompletionIdsTests {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final String GIGACHAT_RESPONSE = """
			{"choices":[{"message":{"content":"Hi!","role":"assistant"},"index":0,"finish_reason":"stop"}],\
			"created":1791725138,"model":"GigaChat-2:2.0.30.01","object":"chat.completions",\
			"usage":{"prompt_tokens":21,"completion_tokens":4,"total_tokens":25}}""";

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в ответ GigaChat без id добавляется id")
	void injectsIdWhenMissing() throws Exception {
		//Act
		String result = ChatCompletionIds.ensureId(GIGACHAT_RESPONSE, "chatcmpl-test");

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node.path("id").asText()).isEqualTo("chatcmpl-test");
		assertThat(withoutId(node)).isEqualTo(MAPPER.readTree(GIGACHAT_RESPONSE));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ OpenAI со своим id не меняется")
	void keepsExistingId() throws Exception {
		//Arrange
		String withId = GIGACHAT_RESPONSE.replace("{", "{\"id\":\"chatcmpl-original\",");

		//Act
		String result = ChatCompletionIds.ensureId(withId, "chatcmpl-test");

		//Assert
		assertThat(MAPPER.readTree(result)).isEqualTo(MAPPER.readTree(withId));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что JSON без choices не меняется")
	void leavesOtherObjectsUntouched() {
		//Arrange
		String error = "{\"error\":{\"code\":429,\"message\":\"Too many requests\"}}";

		//Act
		String result = ChatCompletionIds.ensureId(error);

		//Assert
		assertThat(result).isEqualTo(error);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что массив JSON (например, список моделей) не меняется")
	void leavesArraysUntouched() {
		//Arrange
		String models = "[{\"id\":\"GigaChat-2\",\"object\":\"model\"}]";

		//Act
		String result = ChatCompletionIds.ensureId(models);

		//Assert
		assertThat(result).isEqualTo(models);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неразбираемый JSON возвращается как есть")
	void leavesInvalidJsonUntouched() {
		//Arrange
		String invalid = "not a json";

		//Act
		String result = ChatCompletionIds.ensureId(invalid);

		//Assert
		assertThat(result).isEqualTo(invalid);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неэкранированные управляющие символы из ответа GigaChat исправляются")
	void escapesRawControlCharacters() throws Exception {
		//Arrange
		String messy = "{\"choices\":[{\"message\":{\"content\":\"Line1\nLine2\",\"role\":\"assistant\"}}],"
				+ "\"created\":1,\"object\":\"chat.completions\"}";

		//Act
		String result = ChatCompletionIds.ensureId(messy, "chatcmpl-test");

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node.path("id").asText()).isEqualTo("chatcmpl-test");
		assertThat(node.at("/choices/0/message/content").asText()).isEqualTo("Line1\nLine2");
	}

	private ObjectNode withoutId(JsonNode node) {
		ObjectNode copy = node.deepCopy();
		copy.remove("id");
		return copy;
	}

}