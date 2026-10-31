package com.makhov_pet_projects.textik_v_1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.ChatMessage;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.ChatRole;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ChatMessageRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.SessionService;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Поток сессии: обсуждение")
class DiscussFlowTests extends WebTest {

	private static final GeneratedText TEXT = new GeneratedText("Sleep and Health", "Why sleep matters",
			"Sleep is one of the most important habits for health.");

	private static final List<GeneratedQuestion> QUESTIONS = List.of(
			new GeneratedQuestion("Why is sleep important?", List.of("Health", "Money", "Traffic", "Weather"),
					"Health", "Sleep supports health."));

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private ChatMessageRepository chatMessages;

	@Autowired
	private SessionService sessionService;

	@MockitoBean
	private AiGateway ai;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что первый заход начинает обсуждение вопросом ИИ")
	void firstVisitStartsDiscussionWithAiQuestion() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("What did you think about the text?")))
		.andExpect(content().string(containsString("Go to review")));

		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.DISCUSS);
		assertThat(chatMessages.findBySessionIdOrderById(session.getId()))
				.extracting(ChatMessage::getContent)
				.containsExactly("What did you think about the text?");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что повторный заход не дублирует вступительный вопрос")
	void secondVisitDoesNotDuplicateOpening() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk());

		assertThat(chatMessages.findBySessionIdOrderById(session.getId())).hasSize(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сообщение ученика и ответ ИИ сохраняются по порядку")
	void sendingMessageSavesUserAndAssistantMessages() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Great answer. What else did you notice?");
		when(ai.correctUserMessage(any(), any())).thenReturn("I really liked the part about habits.");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions/{id}/discuss", session.getId())
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("message", "I liked the part about habits."))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss"));

		List<ChatMessage> messages = chatMessages.findBySessionIdOrderById(session.getId());
		assertThat(messages).extracting(ChatMessage::getContent).containsExactly(
				"What did you think about the text?",
				"I liked the part about habits.",
				"Great answer. What else did you notice?");
		assertThat(messages).extracting(ChatMessage::getRole).containsExactly(
				ChatRole.ASSISTANT, ChatRole.USER, ChatRole.ASSISTANT);
		assertThat(messages).extracting(ChatMessage::getCorrection).containsExactly(
				null, "I really liked the part about habits.", null);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что исправление показывается под репликой ученика")
	void showsCorrectionUnderUserMessage() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Great answer. What else did you notice?");
		when(ai.correctUserMessage(any(), any())).thenReturn("I really liked the part about habits.");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		sendMessage(session, user, "I liked the part about habits.").andExpect(status().is3xxRedirection());

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("I liked the part about habits.")))
		.andExpect(content().string(containsString("Corrected")))
		.andExpect(content().string(containsString("I really liked the part about habits.")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что для корректной реплики показывается подтверждение")
	void showsConfirmationForCorrectMessage() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Good point. What else?");
		when(ai.correctUserMessage(any(), any())).thenReturn("");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		sendMessage(session, user, "I liked the part about habits.").andExpect(status().is3xxRedirection());

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("no changes needed")));

		List<ChatMessage> messages = chatMessages.findBySessionIdOrderById(session.getId());
		assertThat(messages.get(1).getCorrection()).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пустое сообщение не сохраняется и ведёт к ошибке")
	void blankMessageRedirectsWithErrorAndSavesNothing() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions/{id}/discuss", session.getId())
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("message", "   "))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss?error=1"));

		assertThat(chatMessages.findBySessionIdOrderById(session.getId())).hasSize(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что после первой реплики показывается, сколько осталось отправить")
	void showsRemainingMessagesBeforeReview() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Good point. What else?");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		sendMessage(session, user, "First thought").andExpect(status().is3xxRedirection());

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Send 2 more message(s) to finish the discussion.")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что после трёх реплик обсуждение считается завершённым")
	void unlocksReviewAfterThreeUserTurns() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Good point. What else?");
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		sendMessage(session, user, "First thought").andExpect(status().is3xxRedirection());
		sendMessage(session, user, "Second thought").andExpect(status().is3xxRedirection());
		sendMessage(session, user, "Third thought").andExpect(status().is3xxRedirection());

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("You have finished the discussion.")))
		.andExpect(content().string(containsString("Go to review")))
		.andExpect(content().string(containsString("/sessions/" + session.getId() + "/review")));

		assertThat(chatMessages.countBySessionIdAndRole(session.getId(), ChatRole.USER)).isEqualTo(3);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой ИИ при ответе показывает ошибку и ничего не сохраняет")
	void replyFailureShowsErrorAndSavesNothing() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenThrow(new AiGatewayException("endpoint недоступен"));
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		sendMessage(session, user, "I liked the part about habits.")

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss?error=1"));

		assertThat(chatMessages.findBySessionIdOrderById(session.getId())).hasSize(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой исправления не отменяет сохранённый ответ")
	void correctionFailureKeepsReplyWithoutCorrection() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Good point. What else?");
		when(ai.correctUserMessage(any(), any())).thenThrow(new AiGatewayException("редактор недоступен"));
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		sendMessage(session, user, "I liked the part about habits.")

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss"));

		List<ChatMessage> messages = chatMessages.findBySessionIdOrderById(session.getId());
		assertThat(messages).extracting(ChatMessage::getContent).containsExactly(
				"What did you think about the text?",
				"I liked the part about habits.",
				"Good point. What else?");
		assertThat(messages.get(1).getCorrection()).isNull();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой ИИ при старте показывает ошибку без цикла перезагрузки")
	void openingFailureShowsErrorWithoutRedirectLoop() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(user);
		when(ai.openDiscussion(any())).thenThrow(new AiGatewayException("endpoint недоступен"));

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("The AI could not reply. Please try again.")));

		assertThat(chatMessages.findBySessionIdOrderById(session.getId())).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чужая сессия не открывается в обсуждении")
	void foreignSessionIsNotFound() throws Exception {
		//Arrange
		AppUser owner = TestFixtures.userWithProfile(users, profiles);
		AppUser stranger = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtThink(owner);

		//Act
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(stranger))))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/sessions/{id}/discuss", session.getId())
						.with(user(AppUserDetails.of(stranger)))
						.with(csrf())
						.param("message", "Hello"))

		//Assert
		.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что после вопросов страница ведёт к обсуждению")
	void thinkPageLinksToDiscussion() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionService.createWithContent(user.getId(), "A1", TEXT, QUESTIONS);
		sessionService.startThinking(session.getId(), user.getId());
		Map<Long, String> answers = new HashMap<>();
		session.getQuestions().forEach(question -> answers.put(question.getId(), question.getCorrectAnswer()));
		sessionService.answerQuestions(session.getId(), user.getId(), answers);

		//Act
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("To discussion")))
		.andExpect(content().string(containsString("/sessions/" + session.getId() + "/discuss")));
	}

	private LearningSession sessionAtThink(AppUser user) {
		LearningSession session = LearningSession.proposed(user, "A1");
		session.setTitle("Sleep and Health");
		session.setDescription("Why sleep matters");
		session.setTextContent(TEXT.text());
		session.advance();
		session.advance();
		return sessions.saveAndFlush(session);
	}

	private ResultActions sendMessage(LearningSession session, AppUser user, String message) throws Exception {
		return mockMvc.perform(post("/sessions/{id}/discuss", session.getId())
				.with(user(AppUserDetails.of(user)))
				.with(csrf())
				.param("message", message));
	}

}
