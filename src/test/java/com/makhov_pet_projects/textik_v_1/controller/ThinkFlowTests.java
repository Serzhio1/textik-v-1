package com.makhov_pet_projects.textik_v_1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Question;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.repository.QuestionRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.SessionService;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

@DisplayName("Поток сессии: вопросы")
class ThinkFlowTests extends WebTest {

	private static final GeneratedText TEXT = new GeneratedText("Sleep and Health", "Why sleep matters",
			"Sleep is one of the most important habits for health.");

	private static final List<GeneratedQuestion> QUESTIONS = List.of(
			new GeneratedQuestion("Why is sleep important?", List.of("Health", "Money", "Traffic", "Weather"),
					"Health", "Sleep supports health."),
			new GeneratedQuestion("How many hours are recommended?", List.of("3-4", "5-6", "7-9", "12"),
					"7-9", "Adults need 7-9 hours."));

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Autowired
	private SessionRepository sessions;

	@Autowired
	private QuestionRepository questions;

	@Autowired
	private SessionService sessionService;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что страница вопросов показывает все вопросы и переводит сессию в THINK")
	void thinkPageShowsAllQuestionsAndStartsThinking() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);

		//Act
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Why is sleep important?")))
		.andExpect(content().string(containsString("How many hours are recommended?")));

		assertThat(sessions.findById(session.getId()).orElseThrow().getStatus()).isEqualTo(SessionStatus.THINK);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что все правильные ответы дают счёт и объяснения")
	void correctAnswersShowScoreAndExplanations() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		answerAll(session, user, "Health", "7-9")
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/sessions/" + session.getId() + "/think"));

		//Assert
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("2 / 2 correct")))
				.andExpect(content().string(containsString("Sleep supports health.")))
				.andExpect(content().string(containsString("Adults need 7-9 hours.")));

		assertThat(questions.findBySessionIdOrderById(session.getId()))
				.extracting(Question::getIsCorrect).containsExactly(true, true);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неверный ответ показывает объяснение и число ошибок")
	void wrongAnswerShowsExplanationAndWrongCount() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		answerAll(session, user, "Money", "7-9")
				.andExpect(status().is3xxRedirection());

		//Assert
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("1 / 2 correct")))
				.andExpect(content().string(containsString("1 wrong")))
				.andExpect(content().string(containsString("Sleep supports health.")));

		assertThat(questions.findBySessionIdOrderById(session.getId()))
				.extracting(Question::getIsCorrect).containsExactly(false, true);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что без ответа на все вопросы ничего не сохраняется")
	void missingAnswerRedirectsWithErrorAndSavesNothing() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		mockMvc.perform(post("/sessions/{id}/think", session.getId())
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("answer_" + session.getQuestions().getFirst().getId(), "Health"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/think?error=1"));

		assertThat(questions.findBySessionIdOrderById(session.getId())).noneMatch(Question::isAnswered);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что вариант не из списка отклоняется и ничего не сохраняется")
	void invalidOptionRedirectsWithErrorAndSavesNothing() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());

		//Act
		answerAll(session, user, "Not an option", "Not an option")

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/think?error=1"));

		assertThat(questions.findBySessionIdOrderById(session.getId())).noneMatch(Question::isAnswered);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чужая сессия не открывается на странице вопросов")
	void foreignSessionIsNotFound() throws Exception {
		//Arrange
		AppUser owner = TestFixtures.userWithProfile(users, profiles);
		AppUser stranger = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(owner);

		//Act
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(stranger))))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/sessions/{id}/think", session.getId())
						.with(user(AppUserDetails.of(stranger)))
						.with(csrf())
						.param("answer_" + session.getQuestions().getFirst().getId(), "Health"))

		//Assert
		.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что повторная отправка не перезаписывает ответы")
	void completedSessionIsNotOverwritten() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);
		mockMvc.perform(get("/sessions/{id}/think", session.getId()).with(user(AppUserDetails.of(user))))
				.andExpect(status().isOk());
		answerAll(session, user, "Health", "7-9").andExpect(status().is3xxRedirection());

		//Act
		answerAll(session, user, "Money", "3-4")

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/sessions/" + session.getId() + "/think"));

		assertThat(questions.findBySessionIdOrderById(session.getId()))
				.extracting(Question::getUserAnswer).containsExactly("Health", "7-9");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что страница чтения ведёт к вопросам")
	void readPageLinksToQuestions() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		LearningSession session = createSession(user);

		//Act
		mockMvc.perform(get("/sessions/{id}/read", session.getId()).with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Go to questions")))
		.andExpect(content().string(containsString("/sessions/" + session.getId() + "/think")));
	}

	private LearningSession createSession(AppUser user) {
		return sessionService.createWithContent(user.getId(), "A1", TEXT, QUESTIONS);
	}

	private ResultActions answerAll(LearningSession session, AppUser user, String first, String second)
			throws Exception {
		return mockMvc.perform(post("/sessions/{id}/think", session.getId())
				.with(user(AppUserDetails.of(user)))
				.with(csrf())
				.param("answer_" + session.getQuestions().get(0).getId(), first)
				.param("answer_" + session.getQuestions().get(1).getId(), second));
	}

}
