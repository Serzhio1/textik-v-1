package com.makhov_pet_projects.textik_v_1.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.makhov_pet_projects.textik_v_1.domain.AppUser;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import com.makhov_pet_projects.textik_v_1.support.WebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Регистрация")
class RegistrationTests extends WebTest {

	private static final String PASSWORD = "correct-horse";

	@Autowired
	private AppUserRepository users;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что страница регистрации отдаёт форму")
	void showsRegistrationForm() throws Exception {
		//Act
		mockMvc.perform(get("/register"))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("name=\"email\"")))
		.andExpect(content().string(containsString("name=\"passwordConfirm\"")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что после регистрации пользователь сразу попадает на главный экран")
	void logsInRightAfterRegistration() throws Exception {
		//Arrange
		String email = TestFixtures.randomEmail();

		//Act
		mockMvc.perform(post("/register").with(csrf())
						.param("email", email)
						.param("username", "Sergey")
						.param("password", PASSWORD)
						.param("passwordConfirm", PASSWORD))

		//Assert
		.andExpect(status().is3xxRedirection())
		.andExpect(redirectedUrl("/"))
		.andExpect(authenticated().withUsername(email));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новый пользователь сохраняется с BCrypt-хэшем пароля")
	void storesPasswordAsBcryptHash() throws Exception {
		//Arrange
		String email = TestFixtures.randomEmail();

		//Act
		mockMvc.perform(post("/register").with(csrf())
				.param("email", email)
				.param("username", "Sergey")
				.param("password", PASSWORD)
				.param("passwordConfirm", PASSWORD));

		//Assert
		AppUser stored = users.findByEmailIgnoreCase(email).orElseThrow();
		assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
		assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что занятый email показывает ошибку, а не падает")
	void reportsEmailAlreadyTaken() throws Exception {
		//Arrange
		AppUser existing = TestFixtures.user(users);

		//Act
		mockMvc.perform(post("/register").with(csrf())
				.param("email", existing.getEmail())
				.param("username", "Sergey")
				.param("password", PASSWORD)
				.param("passwordConfirm", PASSWORD))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Такой email уже зарегистрирован")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что некорректный email отклоняется формой")
	void rejectsMalformedEmail() throws Exception {
		//Act
		mockMvc.perform(post("/register").with(csrf())
				.param("email", "not-an-email")
				.param("username", "Sergey")
				.param("password", PASSWORD)
				.param("passwordConfirm", PASSWORD))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Некорректный email")));
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что несовпадение паролей отклоняется формой")
	void rejectsPasswordMismatch() throws Exception {
		//Act
		mockMvc.perform(post("/register").with(csrf())
				.param("email", TestFixtures.randomEmail())
				.param("username", "Sergey")
				.param("password", PASSWORD)
				.param("passwordConfirm", "other-horse"))

		//Assert
		.andExpect(status().isOk())
		.andExpect(content().string(containsString("Пароли не совпадают")));
	}

}
