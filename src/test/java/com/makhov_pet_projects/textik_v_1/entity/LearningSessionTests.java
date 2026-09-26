package com.makhov_pet_projects.textik_v_1.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.enums.ChatRole;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Учебная сессия")
class LearningSessionTests {

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новая сессия создаётся в статусе PROPOSED")
	void startsInProposedStatus() {
		//Arrange
		AppUser user = new AppUser("sergey@example.com", "Sergey", "bcrypt-hash");

		//Act
		LearningSession session = LearningSession.proposed(user, "A2");

		//Assert
		assertThat(session.getStatus()).isEqualTo(SessionStatus.PROPOSED);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что уровень сессии наследуется из профиля")
	void keepsProfileLevel() {
		//Arrange
		AppUser user = new AppUser("sergey@example.com", "Sergey", "bcrypt-hash");

		//Act
		LearningSession session = LearningSession.proposed(user, "B1");

		//Assert
		assertThat(session.getLevel()).isEqualTo("B1");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что метод advance двигает сессию на шаг вперёд")
	void advanceMovesStatusForward() {
		//Arrange
		LearningSession session = proposedSession();

		//Act
		session.advance();

		//Assert
		assertThat(session.getStatus()).isEqualTo(SessionStatus.READ);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что добавленный вопрос попадает в коллекцию вопросов сессии")
	void addsQuestionToSession() {
		//Arrange
		LearningSession session = proposedSession();

		//Act
		session.addQuestion("Pick one", List.of("a", "b", "c", "d"), "c", "Потому что c");

		//Assert
		assertThat(session.getQuestions()).hasSize(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что реплика пользователя сохраняется с ролью USER")
	void addsUserMessageWithUserRole() {
		//Arrange
		LearningSession session = proposedSession();

		//Act
		session.addUserMessage("Почему сон важен?");

		//Assert
		assertThat(session.getChatMessages()).first()
				.extracting(ChatMessage::getRole).isEqualTo(ChatRole.USER);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что реплика ИИ сохраняется с ролью ASSISTANT")
	void addsAssistantMessageWithAssistantRole() {
		//Arrange
		LearningSession session = proposedSession();

		//Act
		session.addAssistantMessage("Потому что он влияет на память.");

		//Assert
		assertThat(session.getChatMessages()).first()
				.extracting(ChatMessage::getRole).isEqualTo(ChatRole.ASSISTANT);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что число реплик пользователя считается отдельно от реплик ИИ")
	void countsOnlyUserTurns() {
		//Arrange
		LearningSession session = proposedSession();
		session.addUserMessage("Первый вопрос");
		session.addAssistantMessage("Первый ответ");
		session.addUserMessage("Второй вопрос");
		session.addAssistantMessage("Второй ответ");

		//Act
		long userTurns = session.userTurns();

		//Assert
		assertThat(userTurns).isEqualTo(2);
	}

	private static LearningSession proposedSession() {
		AppUser user = new AppUser("sergey@example.com", "Sergey", "bcrypt-hash");
		return LearningSession.proposed(user, "A1");
	}

}
