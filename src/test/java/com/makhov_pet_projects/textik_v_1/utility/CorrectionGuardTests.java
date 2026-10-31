package com.makhov_pet_projects.textik_v_1.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Защита от неправдоподобных исправлений")
class CorrectionGuardTests {

	@Test
	@DisplayName("Тест, в котором мы проверяем, что минимальная правка сообщения принимается")
	void acceptsMinimalCorrection() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible("i has a question", "I have a question.");

		//Assert
		assertThat(plausible).isTrue();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ-уточнение вместо исправления отклоняется")
	void rejectsAnswerInsteadOfCorrection() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible(
				"i dont understand your question. Doy ask this more clearly?",
				"Sure, I'd love to clarify my question. What factors matter most to you?");

		//Assert
		assertThat(plausible).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что исправление с лишними идеями отклоняется")
	void rejectsCorrectionWithAddedIdeas() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible(
				"can you ask me another question?",
				"Can you ask me another question about the benefits mentioned in the text?");

		//Assert
		assertThat(plausible).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что короткая правка сокращения принимается")
	void acceptsShortAbbreviationFix() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible("dont", "do not");

		//Assert
		assertThat(plausible).isTrue();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что короткий ответ не превращается в полный")
	void rejectsExpandedShortAnswer() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible(
				"Yes", "Yes, I read every day because it helps me.");

		//Assert
		assertThat(plausible).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что текст без изменений не считается исправлением")
	void rejectsUnchangedText() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible(
				"I have a question.", "I have a question.");

		//Assert
		assertThat(plausible).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пустое исправление считается допустимым")
	void acceptsEmptyCorrection() {
		//Act
		boolean plausible = CorrectionGuard.isPlausible("I have a question.", "");

		//Assert
		assertThat(plausible).isTrue();
	}

}
