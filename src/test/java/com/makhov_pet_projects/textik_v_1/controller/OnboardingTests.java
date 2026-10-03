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

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.ProfileRepository;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Онбординг")
class OnboardingTests extends WebTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новый пользователь видит пустую форму профиля")
	void showsEmptyFormToNewUser() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(get("/onboarding").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("name=\"about\"")))
		.andExpect(content().string(containsString("name=\"interests\"")))
		.andExpect(content().string(containsString("name=\"level\"")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пользователя без профиля отправляет на онбординг")
	void redirectsUserWithoutProfileToOnboarding() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(get("/").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/onboarding"));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пользователь с профилем попадает на главный экран")
	void letsProfiledUserIntoHome() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		mockMvc.perform(get("/").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString(user.getUsername())));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что заполненный профиль сохраняется и открывает главный экран")
	void savesProfileAndRedirectsHome() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(post("/onboarding")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("about", "Читаю статьи по вечерам")
						.param("interests", " технологии , спорт ,, ")
						.param("level", "A2"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/"));

		Profile saved = profiles.findById(user.getId()).orElseThrow();
		assertThat(saved.getAbout()).isEqualTo("Читаю статьи по вечерам");
		assertThat(saved.getInterests()).isEqualTo("технологии, спорт");
		assertThat(saved.getLevel()).isEqualTo("A2");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пустые поля не проходят валидацию и профиль не создаётся")
	void rejectsBlankFields() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(post("/onboarding")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("about", " ")
						.param("interests", " ")
						.param("level", "A1"))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Расскажите о себе")));

		assertThat(profiles.findById(user.getId())).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неизвестный уровень не проходит валидацию")
	void rejectsUnknownLevel() throws Exception {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		mockMvc.perform(post("/onboarding")
						.with(user(AppUserDetails.of(user)))
						.with(csrf())
						.param("about", "Люблю IT")
						.param("interests", "технологии")
						.param("level", "C2"))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Выберите уровень A1, A2 или B1")));

		assertThat(profiles.findById(user.getId())).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пользователь с профилем не заходит на онбординг повторно")
	void redirectsProfiledUserToProgress() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		mockMvc.perform(get("/onboarding").with(user(AppUserDetails.of(user))))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/progress"));
	}

}
