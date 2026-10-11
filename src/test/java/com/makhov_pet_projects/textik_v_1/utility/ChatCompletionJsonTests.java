package com.makhov_pet_projects.textik_v_1.utility;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Починка JSON-ответа GigaChat")
class ChatCompletionJsonTests {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сырые переносы строк в JSON внутри кодового блока экранируются")
	void cleansFencedJsonWithRawNewlines() throws Exception {
		//Arrange
		String messy = """
				```json
				{
				  "title": "Notetaking That Works",
				  "description": "О чём текст",
				  "text": "First paragraph.
				Second paragraph."
				}
				```""";

		//Act
		String result = ChatCompletionJson.sanitize(messy);

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node.path("title").asText()).isEqualTo("Notetaking That Works");
		assertThat(node.path("text").asText()).isEqualTo("First paragraph.\nSecond paragraph.");
		assertThat(result).doesNotStartWith("```");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сырые переносы строк в JSON без кодового блока экранируются")
	void cleansBareJsonWithRawNewlines() throws Exception {
		//Arrange
		String messy = "{\"title\": \"A\", \"text\": \"one\ntwo\"}";

		//Act
		String result = ChatCompletionJson.sanitize(messy);

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node.path("title").asText()).isEqualTo("A");
		assertThat(node.path("text").asText()).isEqualTo("one\ntwo");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что массив вопросов с сырым переносом строка чинится")
	void cleansJsonArrayWithRawNewlines() throws Exception {
		//Arrange
		String messy = "[{\"question\": \"Q\", \"options\": [\"A\", \"B\", \"C\", \"D\"], "
				+ "\"correctAnswer\": \"A\", \"explanation\": \"Первое объяснение\"},\n"
				+ "{\"question\": \"Q2\", \"options\": [\"A\", \"B\", \"C\", \"D\"], "
				+ "\"correctAnswer\": \"B\", \"explanation\": \"Второе\nобъяснение\"}]";

		//Act
		String result = ChatCompletionJson.sanitize(messy);

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node).hasSize(2);
		assertThat(node.path(1).path("explanation").asText()).isEqualTo("Второе\nобъяснение");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что дегенеративный ответ строкой не меняется")
	void keepsDegenerateStringUntouched() {
		//Arrange
		String degenerate = "\"description\"";

		//Act
		String result = ChatCompletionJson.sanitize(degenerate);

		//Assert
		assertThat(result).isEqualTo(degenerate);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что обычный текст без JSON не меняется")
	void leavesPlainTextUntouched() {
		//Arrange
		String prose = "извините, я не смог подобрать текст";

		//Act
		String result = ChatCompletionJson.sanitize(prose);

		//Assert
		assertThat(result).isEqualTo(prose);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что корректный JSON в кодовом блоке очищается от обрамления")
	void validatesCleanFencedJson() throws Exception {
		//Arrange
		String fenced = "```json\n{\"title\": \"A\", \"text\": \"Hello\"}\n```";

		//Act
		String result = ChatCompletionJson.sanitize(fenced);

		//Assert
		JsonNode node = MAPPER.readTree(result);
		assertThat(node.path("title").asText()).isEqualTo("A");
		assertThat(result).doesNotStartWith("```");
	}

}