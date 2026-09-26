package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий пользователей")
class AppUserRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что новый пользователь получает идентификатор")
	void assignsIdToNewUser() {
		//Arrange
		AppUser user = new AppUser(randomEmail(), "Sergey", "bcrypt-hash");

		//Act
		AppUser saved = users.saveAndFlush(user);

		//Assert
		assertThat(saved.getId()).isNotNull();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пользователь ищется по email без учёта регистра")
	void findsUserByEmailIgnoringCase() {
		//Arrange
		String email = randomEmail();
		users.saveAndFlush(new AppUser(email, "Sergey", "bcrypt-hash"));

		//Act
		Optional<AppUser> found = users.findByEmailIgnoreCase(email.toUpperCase());

		//Assert
		assertThat(found).isPresent();
		assertThat(found.get().getUsername()).isEqualTo("Sergey");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что занятый email находится через existsByEmailIgnoreCase")
	void detectsExistingEmail() {
		//Arrange
		String email = randomEmail();
		users.saveAndFlush(new AppUser(email, "Sergey", "bcrypt-hash"));

		//Act
		boolean exists = users.existsByEmailIgnoreCase(email);

		//Assert
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что незанятый email не находится через existsByEmailIgnoreCase")
	void detectsFreeEmail() {
		//Arrange
		String freeEmail = randomEmail();

		//Act
		boolean exists = users.existsByEmailIgnoreCase(freeEmail);

		//Assert
		assertThat(exists).isFalse();
	}

	private static String randomEmail() {
		return UUID.randomUUID() + "@example.com";
	}

}
