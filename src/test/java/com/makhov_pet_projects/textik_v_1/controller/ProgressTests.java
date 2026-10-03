package com.makhov_pet_projects.textik_v_1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Прогресс и профиль")
class ProgressTests extends WebTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Autowired
	private SessionRepository sessions;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что на странице прогресса видны имя, email и уровень")
	void showsProfileData() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		mockMvc.perform(get("/progress").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString(user.getEmail())))
		.andExpect(content().string(containsString("B1")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пользователя без профиля отправляет на онбординг")
	void redirectsUserWithoutProfileToOnboarding() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(get("/progress").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/onboarding"));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сохранённые изменения попадают в базу")
	void savesChanges() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		mockMvc.perform(post("/progress")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("about", "Теперь читаю книги")
						.param("interests", "книги ,, музыка")
						.param("level", "B1"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/progress?saved"));

		Profile saved = profiles.findById(user.getId()).orElseThrow();
		assertThat(saved.getAbout()).isEqualTo("Теперь читаю книги");
		assertThat(saved.getInterests()).isEqualTo("книги, музыка");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что невалидная форма не затирает сохранённый профиль")
	void keepsOldProfileWhenFormInvalid() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		mockMvc.perform(post("/progress")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("about", "")
						.param("interests", "книги")
						.param("level", "A1"))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Расскажите о себе")));

		assertThat(profiles.findById(user.getId()).orElseThrow().getAbout()).isEqualTo("Люблю IT и спорт");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в истории видны завершённые сессии")
	void showsSessionHistory() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);
		TestFixtures.session(sessions, user, "Отпуск в Англии");

		//Act
		mockMvc.perform(get("/progress").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Отпуск в Англии")))
		.andExpect(content().string(not(containsString("Здесь появятся ваши сессии"))));
	}

}
