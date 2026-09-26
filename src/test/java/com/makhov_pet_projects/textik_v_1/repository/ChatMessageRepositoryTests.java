package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.ChatMessage;
import com.makhov_pet_projects.textik_v_1.enums.ChatRole;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий чатов обсуждений")
class ChatMessageRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private ChatMessageRepository chatMessages;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что переписка отдаётся в хронологическом порядке")
	void findsMessagesInChronologicalOrder() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addUserMessage("Почему сон важен?");
		session.addAssistantMessage("Потому что он влияет на память.");
		sessions.saveAndFlush(session);

		//Act
		List<ChatMessage> messages = chatMessages.findBySessionIdOrderById(session.getId());

		//Assert
		assertThat(messages).extracting(ChatMessage::getRole)
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что число реплик пользователя считается для лимита обсуждения")
	void countsUserTurns() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addUserMessage("Первый вопрос");
		session.addAssistantMessage("Первый ответ");
		session.addUserMessage("Второй вопрос");
		sessions.saveAndFlush(session);

		//Act
		long userTurns = chatMessages.countBySessionIdAndRole(session.getId(), ChatRole.USER);

		//Assert
		assertThat(userTurns).isEqualTo(2);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что текст реплики ИИ сохраняется")
	void storesAssistantMessage() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = LearningSession.proposed(user, "A1");
		session.addAssistantMessage("Давай вернёмся к тексту.");
		sessions.saveAndFlush(session);

		//Act
		ChatMessage stored = chatMessages.findBySessionIdOrderById(session.getId()).getFirst();

		//Assert
		assertThat(stored.getContent()).isEqualTo("Давай вернёмся к тексту.");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что у сессии без переписки ноль реплик")
	void findsNoMessagesForNewSession() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		LearningSession session = sessions.saveAndFlush(LearningSession.proposed(user, "A1"));

		//Act
		List<ChatMessage> messages = chatMessages.findBySessionIdOrderById(session.getId());

		//Assert
		assertThat(messages).isEmpty();
	}

}
