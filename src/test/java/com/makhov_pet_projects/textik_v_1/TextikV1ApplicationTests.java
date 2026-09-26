package com.makhov_pet_projects.textik_v_1;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Контекст приложения")
class TextikV1ApplicationTests extends PostgresIntegrationTest {

	@Autowired
	private AppUserRepository users;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что схема базы создана миграциями и репозиторий отвечает")
	void appliesDatabaseSchema() {
		//Arrange
		AppUserRepository repository = users;

		//Act
		long usersInDatabase = repository.count();

		//Assert
		assertThat(usersInDatabase).isZero();
	}

}
