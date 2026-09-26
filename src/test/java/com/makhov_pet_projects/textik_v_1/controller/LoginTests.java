package com.makhov_pet_projects.textik_v_1.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Вход и выход")
class LoginTests extends WebTest {

	private static final String PASSWORD = "correct-horse";

	@Autowired
	private AppUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что анонимного пользователя отправляет на страницу входа")
	void redirectsAnonymousUserToLoginPage() throws Exception {
		//Act
		mockMvc.perform(get("/"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/login"));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что верный email и пароль пускают на главный экран")
	void letsUserLogIn() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithPassword(users, passwordEncoder, PASSWORD);

		//Act
		mockMvc.perform(post("/login").with(csrf())
				.param("email", user.getEmail())
				.param("password", PASSWORD))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/"));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неверный пароль возвращает на форму входа с ошибкой")
	void reportsWrongPassword() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithPassword(users, passwordEncoder, PASSWORD);

		//Act
		mockMvc.perform(post("/login").with(csrf())
				.param("email", user.getEmail())
				.param("password", "wrong-password"))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/login?error"));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что выход оставляет пользователя анонимным")
	void logsUserOut() throws Exception {
		//Arrange
		AppUser user = TestFixtures.userWithPassword(users, passwordEncoder, PASSWORD);
		MockHttpSession session = (MockHttpSession) mockMvc
				.perform(post("/login").with(csrf())
						.param("email", user.getEmail())
						.param("password", PASSWORD))
				.andReturn()
				.getRequest()
				.getSession(false);

		//Act
		mockMvc.perform(post("/logout").with(csrf()).session(session));

		//Assert
		mockMvc.perform(get("/").session(session))
				.andExpect(unauthenticated());
	}

}
