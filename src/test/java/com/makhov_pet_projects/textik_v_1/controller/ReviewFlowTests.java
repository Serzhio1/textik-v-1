package com.makhov_pet_projects.textik_v_1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Question;
import com.makhov_pet_projects.textik_v_1.entity.SessionReview;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.repository.QuestionRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionReviewRepository;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DisplayName("Поток сессии: итоги")
class ReviewFlowTests extends WebTest {

	private static final List<TopicProposal> TOPICS = List.of(
			new TopicProposal("Sleep and Health", "Why sleep matters"));

	private static final GeneratedText TEXT = new GeneratedText("Sleep and Health", "Why sleep matters",
			"Sleep is one of the most important habits for health.");

	private static final List<GeneratedQuestion> QUESTIONS = List.of(
			new GeneratedQuestion("Why is sleep important?", List.of("Health", "Money", "Traffic", "Weather"),
					"Health", "Sleep supports health."),
			new GeneratedQuestion("How many hours are recommended?", List.of("3-4", "5-6", "7-9", "12"),
					"7-9", "Adults need 7-9 hours."));

	private static final ReviewData REVIEW = new ReviewData(
			"You read about why sleep is important for health.",
			"You stayed on topic and explained your ideas clearly.",
			"You sometimes confuse past and present tenses.",
			"A2",
			List.of("Practise past tense forms", "Use articles before singular nouns"));

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private SessionReviewRepository reviews;

	@Autowired
	private QuestionRepository questions;

	@MockitoBean
	private AiGateway ai;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что итоги сохраняются, а сессия получает статус DONE")
	void savesReviewAndFinishesSession() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtDiscuss(user, 3);
		when(ai.generateReview(any())).thenReturn(REVIEW);

		//Act
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Well done!")))
		.andExpect(content().string(containsString(REVIEW.textSummary())))
		.andExpect(content().string(containsString(REVIEW.conversationSummary())))
		.andExpect(content().string(containsString(REVIEW.mistakes())))
		.andExpect(content().string(containsString("1 / 2 correct")))
		.andExpect(content().string(containsString("Practise past tense forms")))
		.andExpect(content().string(containsString("Back to home")));

		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.DONE);
		SessionReview saved = reviews.findBySessionId(session.getId()).orElseThrow();
		assertThat(saved.getTextSummary()).isEqualTo(REVIEW.textSummary());
		assertThat(saved.getConversationSummary()).isEqualTo(REVIEW.conversationSummary());
		assertThat(saved.getMistakes()).isEqualTo(REVIEW.mistakes());
		assertThat(saved.mainErrorLines()).containsExactlyElementsOf(REVIEW.mainErrors());
		assertThat(saved.getConversationGrade()).isEqualTo(REVIEW.conversationGrade());
		assertThat(saved.getQuestionsTotal()).isEqualTo(2);
		assertThat(saved.getQuestionsCorrect()).isEqualTo(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что повторный заход не вызывает модель ещё раз")
	void repeatedVisitReusesSavedReview() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtDiscuss(user, 3);
		when(ai.generateReview(any())).thenReturn(REVIEW);
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk());
		verify(ai, times(1)).generateReview(any());
		assertThat(reviews.findBySessionId(session.getId())).isPresent();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что до трёх реплик итоги не генерируются")
	void tooEarlyReviewRedirectsToDiscussion() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtDiscuss(user, 2);

		//Act
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss"));
		verify(ai, never()).generateReview(any());
		assertThat(reviews.findBySessionId(session.getId())).isEmpty();
		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.DISCUSS);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чужая сессия не открывается на странице итогов")
	void foreignSessionIsNotFound() throws Exception {
		//Arrange
		AppUser owner = TestFixtures.userWithProfile(users, profiles);
		AppUser stranger = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtDiscuss(owner, 3);

		//Act
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(stranger))))

		//Assert
		.andExpect(status().isNotFound());
		verify(ai, never()).generateReview(any());
		assertThat(reviews.findBySessionId(session.getId())).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой модели не завершает сессию и показывает ошибку")
	void reviewFailureKeepsDiscussionAndShowsError() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = sessionAtDiscuss(user, 3);
		when(ai.generateReview(any())).thenThrow(new AiGatewayException("endpoint недоступен"));

		//Act
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/discuss?reviewError=1"));
		assertThat(reviews.findBySessionId(session.getId())).isEmpty();
		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.DISCUSS);

		mockMvc.perform(get("/sessions/{id}/discuss", session.getId())
						.param("reviewError", "true")
						.with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("The AI could not prepare your review.")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем полный путь от чтения до итогов в истории")
	void completesFullSessionFlow() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		when(ai.generateText(any(), any())).thenReturn(TEXT);
		when(ai.generateQuestions(any(), any())).thenReturn(QUESTIONS);
		when(ai.openDiscussion(any())).thenReturn("What did you think about the text?");
		when(ai.continueDiscussion(any(), any())).thenReturn("Good point. What else?");
		when(ai.generateReview(any())).thenReturn(REVIEW);
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))).session(httpSession))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "0"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrlPattern("/sessions/*/read"));
		LearningSession session = sessions.findByUserIdOrderByIdDesc(user.getId()).getFirst();
		mockMvc.perform(get("/sessions/{id}/read", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		List<Question> storedQuestions = questions.findBySessionIdOrderById(session.getId());
		mockMvc.perform(post("/sessions/{id}/think", session.getId())
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("answer_" + storedQuestions.getFirst().getId(), "Health")
						.param("answer_" + storedQuestions.getLast().getId(), "7-9"))
				.andExpect(status().is3xxRedirection());
		mockMvc.perform(get("/sessions/{id}/discuss", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		for (int i = 0; i < 3; i++) {
			mockMvc.perform(post("/sessions/{id}/discuss", session.getId())
							.with(user(AppUserDetails.of(user)))
							.with(csrf())
							.param("message", "Thought number " + i))
					.andExpect(status().is3xxRedirection());
		}
		mockMvc.perform(get("/sessions/{id}/review", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Well done!")));

		//Assert
		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.DONE);
		assertThat(reviews.findBySessionId(session.getId())).isPresent();
		mockMvc.perform(get("/progress").with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Sleep and Health")))
				.andExpect(content().string(containsString("/sessions/" + session.getId() + "/review")));
	}

	private LearningSession sessionAtDiscuss(AppUser user, int userTurns) {
		LearningSession session = LearningSession.proposed(user, "B1");
		session.setTitle("Sleep and Health");
		session.setDescription("Why sleep matters");
		session.setTextContent(TEXT.text());
		session.advance();
		session.startThinking();
		session.startDiscussing();
		session.addQuestion("Why is sleep important?", List.of("Health", "Money", "Traffic", "Weather"),
				"Health", "Sleep supports health.");
		session.addQuestion("How many hours are recommended?", List.of("3-4", "5-6", "7-9", "12"),
				"7-9", "Adults need 7-9 hours.");
		session.getQuestions().getFirst().answer("Health");
		session.getQuestions().getLast().answer("5-6");
		session.addAssistantMessage("What did you think about the text?");
		for (int i = 0; i < userTurns; i++) {
			session.addUserMessage("Thought number " + i, null);
			session.addAssistantMessage("Good point. What else?");
		}
		return sessions.saveAndFlush(session);
	}

}
