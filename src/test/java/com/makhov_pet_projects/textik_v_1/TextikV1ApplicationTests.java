package com.makhov_pet_projects.textik_v_1;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.support.PostgresTestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(PostgresTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Контекст приложения")
class TextikV1ApplicationTests {

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
