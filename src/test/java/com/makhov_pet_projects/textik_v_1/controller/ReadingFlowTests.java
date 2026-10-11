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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.repository.QuestionRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
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

@DisplayName("Поток сессии: выбор темы и чтение")
class ReadingFlowTests extends WebTest {

	private static final List<TopicProposal> TOPICS = List.of(
			new TopicProposal("Reading for Progress", "Почему читать каждый день"),
			new TopicProposal("Notetaking That Works", "Как записывать мысли"),
			new TopicProposal("Small Habits, Big Results", "Почему привычки важны"));

	private static final GeneratedText TEXT = new GeneratedText("Reading for Progress", "Почему читать каждый день",
			"Reading is one of the most reliable ways to improve your English skills.");

	private static final List<GeneratedQuestion> QUESTIONS = List.of(
			new GeneratedQuestion("Почему автор советует читать регулярно?",
					List.of("Это полезно", "Это модно", "Это дёшево", "Это быстро"),
					"Это полезно",
					"Регулярное чтение — основа прогресса."));

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private QuestionRepository questions;

	@MockitoBean
	private AiGateway ai;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что выбор темы показывает три предложенных темы")
	void selectShowsThreeTopics() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);

		//Act
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Reading for Progress")))
		.andExpect(content().string(containsString("Small Habits, Big Results")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что старт чтения сохраняет сессию с текстом и вопросами")
	void startReadingSavesSessionWithTextAndQuestions() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		when(ai.generateText(any(), any())).thenReturn(TEXT);
		when(ai.generateQuestions(any(), any())).thenReturn(QUESTIONS);
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))).session(httpSession))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "0"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrlPattern("/sessions/*/read"));

		LearningSession saved = sessions.findByUserIdOrderByIdDesc(user.getId()).get(0);
		assertThat(saved.getStatus()).isEqualTo(SessionStatus.READ);
		assertThat(saved.getTitle()).isEqualTo("Reading for Progress");
		assertThat(saved.getTextContent()).isEqualTo(TEXT.text());
		assertThat(questions.findBySessionIdOrderById(saved.getId())).hasSize(1);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что страница чтения показывает название и текст")
	void readPageShowsTitleAndText() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		when(ai.generateText(any(), any())).thenReturn(TEXT);
		when(ai.generateQuestions(any(), any())).thenReturn(QUESTIONS);
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))).session(httpSession))
				.andExpect(status().isOk());
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "0"))
				.andExpect(status().is3xxRedirection());
		LearningSession saved = sessions.findByUserIdOrderByIdDesc(user.getId()).get(0);

		//Act
		mockMvc.perform(get("/sessions/{id}/read", saved.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Reading for Progress")))
		.andExpect(content().string(containsString("reliable ways to improve")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чужая сессия не открывается")
	void foreignSessionIsNotFound() throws Exception {
		//Arrange
		AppUser owner = TestFixtures.userWithProfile(users, profiles);
		AppUser other = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		when(ai.generateText(any(), any())).thenReturn(TEXT);
		when(ai.generateQuestions(any(), any())).thenReturn(QUESTIONS);
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(owner))).session(httpSession))
				.andExpect(status().isOk());
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(owner)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "0"))
				.andExpect(status().is3xxRedirection());
		LearningSession saved = sessions.findByUserIdOrderByIdDesc(owner.getId()).get(0);

		//Act
		mockMvc.perform(get("/sessions/{id}/read", saved.getId()).with(user(AppUserDetails.of(other))))

		//Assert
		.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой генерации отдаёт ошибку и ничего не сохраняет")
	void generationFailureRedirectsWithErrorAndSavesNothing() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		when(ai.generateText(any(), any())).thenThrow(new AiGatewayException("endpoint недоступен"));
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))).session(httpSession))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "0"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/select?error=1"));

		assertThat(sessions.count()).isZero();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неверный номер темы ведёт обратно к выбору")
	void invalidTopicIndexRedirectsToSelect() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		when(ai.generateTopics(any())).thenReturn(TOPICS);
		MockHttpSession httpSession = new MockHttpSession();
		mockMvc.perform(get("/select").with(user(AppUserDetails.of(user))).session(httpSession))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.session(httpSession)
						.param("topic", "99"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/select"));
	}

}