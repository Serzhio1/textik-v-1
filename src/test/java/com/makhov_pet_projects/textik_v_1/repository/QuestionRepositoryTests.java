package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.domain.AppUser;
import com.makhov_pet_projects.textik_v_1.domain.LearningSession;
import com.makhov_pet_projects.textik_v_1.domain.Question;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий вопросов")
class QuestionRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private QuestionRepository questions;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что варианты ответа сохраняются в JSONB и читаются без потерь")
	void storesOptionsAsJsonb() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addQuestion("How much sleep is recommended?",
				List.of("5 hours", "7-9 hours", "12 hours", "1 hour"), "7-9 hours", "Взрослым советуют 7-9 часов.");
		sessions.saveAndFlush(session);

		//Act
		Question stored = questions.findBySessionIdOrderById(session.getId()).getFirst();

		//Assert
		assertThat(stored.getOptions()).containsExactly("5 hours", "7-9 hours", "12 hours", "1 hour");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что вопросы сессии отдаются в порядке добавления")
	void findsQuestionsInCreationOrder() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addQuestion("Первый", List.of("a", "b", "c", "d"), "a", null);
		session.addQuestion("Второй", List.of("a", "b", "c", "d"), "b", null);
		sessions.saveAndFlush(session);

		//Act
		List<Question> found = questions.findBySessionIdOrderById(session.getId());

		//Assert
		assertThat(found).extracting(Question::getPrompt).containsExactly("Первый", "Второй");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что правильный ответ с объяснением сохраняется")
	void storesCorrectAnswerWithExplanation() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addQuestion("Pick one", List.of("a", "b", "c", "d"), "c", "Потому что c");
		sessions.saveAndFlush(session);

		//Act
		Question stored = questions.findBySessionIdOrderById(session.getId()).getFirst();

		//Assert
		assertThat(stored.getCorrectAnswer()).isEqualTo("c");
		assertThat(stored.getExplanation()).isEqualTo("Потому что c");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ пользователя и флаг корректности сохраняются")
	void storesUserAnswer() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addQuestion("Pick one", List.of("a", "b", "c", "d"), "c", null);
		sessions.saveAndFlush(session);

		//Act
		Question stored = questions.findBySessionIdOrderById(session.getId()).getFirst();
		stored.answer("a");
		questions.saveAndFlush(stored);

		//Assert
		assertThat(questions.findBySessionIdOrderById(session.getId()).getFirst().getUserAnswer()).isEqualTo("a");
		assertThat(stored.getIsCorrect()).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что число ошибок считается по неверным ответам")
	void countsWrongAnswers() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addQuestion("Первый", List.of("a", "b", "c", "d"), "a", null);
		session.addQuestion("Второй", List.of("a", "b", "c", "d"), "b", null);
		sessions.saveAndFlush(session);
		List<Question> stored = questions.findBySessionIdOrderById(session.getId());

		//Act
		stored.getFirst().answer("b");
		questions.saveAndFlush(stored.getFirst());
		stored.get(1).answer("b");
		questions.saveAndFlush(stored.get(1));
		long wrongAnswers = questions.countBySessionIdAndIsCorrectFalse(session.getId());

		//Assert
		assertThat(wrongAnswers).isEqualTo(1);
	}

}
