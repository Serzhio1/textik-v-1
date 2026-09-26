package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.SessionReview;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий итогов сессии")
class SessionReviewRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private SessionReviewRepository reviews;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что итог сессии сохраняется и находится по id сессии")
	void storesReviewForSession() {
		//Arrange
		LearningSession session = sessionWithTopic();
		SessionReview review = SessionReview.of(session, "Резюме текста", "Резюме разговора", "Ошибки", "B1", 5, 4);

		//Act
		reviews.saveAndFlush(review);

		//Assert
		assertThat(reviews.findBySessionId(session.getId())).isPresent();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что оценка уровня разговора сохраняется")
	void storesConversationGrade() {
		//Arrange
		LearningSession session = sessionWithTopic();
		reviews.saveAndFlush(
				SessionReview.of(session, "Резюме текста", "Резюме разговора", "Ошибки", "A2", 5, 4));

		//Act
		SessionReview stored = reviews.findBySessionId(session.getId()).orElseThrow();

		//Assert
		assertThat(stored.getConversationGrade()).isEqualTo("A2");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что счётчики правильных ответов сохраняются")
	void storesQuestionScore() {
		//Arrange
		LearningSession session = sessionWithTopic();
		reviews.saveAndFlush(
				SessionReview.of(session, "Резюме текста", "Резюме разговора", "Ошибки", "B1", 7, 5));

		//Act
		SessionReview stored = reviews.findBySessionId(session.getId()).orElseThrow();

		//Assert
		assertThat(stored.getQuestionsTotal()).isEqualTo(7);
		assertThat(stored.getQuestionsCorrect()).isEqualTo(5);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что время создания итога проставляется автоматически")
	void fillsCreatedAtAutomatically() {
		//Arrange
		LearningSession session = sessionWithTopic();
		reviews.saveAndFlush(
				SessionReview.of(session, "Резюме текста", "Резюме разговора", "Ошибки", "B1", 5, 4));

		//Act
		SessionReview stored = reviews.findBySessionId(session.getId()).orElseThrow();

		//Assert
		assertThat(stored.getCreatedAt()).isNotNull();
	}

	private LearningSession sessionWithTopic() {
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "B1");
		session.setTitle("Why sleep matters");
		return sessions.saveAndFlush(session);
	}

}
