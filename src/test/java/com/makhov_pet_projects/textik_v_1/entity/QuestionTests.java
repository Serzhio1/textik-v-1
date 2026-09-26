package com.makhov_pet_projects.textik_v_1.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Вопрос сессии")
class QuestionTests {

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новый вопрос ещё не отвечен")
	void isNotAnsweredByDefault() {
		//Arrange
		Question question = singleChoiceQuestion("c");

		//Act
		boolean answered = question.isAnswered();

		//Assert
		assertThat(answered).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что верный ответ помечается как корректный")
	void marksCorrectAnswer() {
		//Arrange
		Question question = singleChoiceQuestion("c");

		//Act
		question.answer("c");

		//Assert
		assertThat(question.getIsCorrect()).isTrue();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неверный ответ помечается как некорректный")
	void marksWrongAnswer() {
		//Arrange
		Question question = singleChoiceQuestion("c");

		//Act
		question.answer("a");

		//Assert
		assertThat(question.getIsCorrect()).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ пользователя сохраняется в вопросе")
	void storesGivenAnswer() {
		//Arrange
		Question question = singleChoiceQuestion("c");

		//Act
		question.answer("a");

		//Assert
		assertThat(question.getUserAnswer()).isEqualTo("a");
	}

	private static Question singleChoiceQuestion(String correctAnswer) {
		AppUser user = new AppUser("sergey@example.com", "Sergey", "bcrypt-hash");
		return Question.singleChoice(LearningSession.proposed(user, "A1"), "Pick one",
				List.of("a", "b", "c", "d"), correctAnswer, null);
	}

}
