package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий учебных сессий")
class SessionRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private SessionRepository sessions;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новая сессия сохраняется в статусе PROPOSED")
	void savesNewSessionWithProposedStatus() {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		LearningSession session = sessions.saveAndFlush(LearningSession.proposed(user, "B1"));

		//Assert
		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.PROPOSED);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что смена статуса сохраняется")
	void savesStatusChange() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.advance();
		sessions.saveAndFlush(session);

		//Act
		session.advance();
		sessions.saveAndFlush(session);

		//Assert
		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.THINK);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что текст и заголовок сессии сохраняются")
	void savesSessionText() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A2");
		session.setTitle("Why sleep matters");
		session.setTextContent("Sleep is one of the most important habits for health.");

		//Act
		sessions.saveAndFlush(session);

		//Assert
		assertThat(sessions.findById(session.getId()).orElseThrow().getTextContent())
				.isEqualTo("Sleep is one of the most important habits for health.");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чужая сессия не находится по id и id пользователя")
	void hidesSessionOfAnotherUser() {
		//Arrange
		AppUser owner = TestFixtures.user(users);
		AppUser stranger = TestFixtures.user(users);
		LearningSession session = sessions.saveAndFlush(LearningSession.proposed(owner, "A1"));

		//Act
		boolean found = sessions.findByIdAndUserId(session.getId(), stranger.getId()).isPresent();

		//Assert
		assertThat(found).isFalse();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что история пользователя отдаётся от свежих к старым")
	void findsUserHistoryInReverseOrder() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession first = sessions.saveAndFlush(LearningSession.proposed(user, "A1"));
		LearningSession second = sessions.saveAndFlush(LearningSession.proposed(user, "A1"));

		//Act
		List<LearningSession> history = sessions.findByUserIdOrderByIdDesc(user.getId());

		//Assert
		assertThat(history).extracting(LearningSession::getId).containsExactly(second.getId(), first.getId());
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что число завершённых сессий считается по статусу")
	void countsSessionsByStatus() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession done = LearningSession.proposed(user, "A1");
		done.advance();
		done.advance();
		done.advance();
		done.advance();
		sessions.saveAndFlush(done);
		sessions.saveAndFlush(LearningSession.proposed(user, "A1"));

		//Act
		long count = sessions.countByUserIdAndStatus(user.getId(), SessionStatus.DONE);

		//Assert
		assertThat(count).isEqualTo(1);
	}

}
