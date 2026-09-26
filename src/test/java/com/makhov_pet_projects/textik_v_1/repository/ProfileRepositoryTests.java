package com.makhov_pet_projects.textik_v_1.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.domain.AppUser;
import com.makhov_pet_projects.textik_v_1.domain.Profile;
import com.makhov_pet_projects.textik_v_1.support.PostgresRepositoryTest;
import com.makhov_pet_projects.textik_v_1.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Репозиторий профилей")
class ProfileRepositoryTests extends PostgresRepositoryTest {

	@Autowired
	private AppUserRepository users;

	@Autowired
	private ProfileRepository profiles;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что профиль сохраняется под идентификатором пользователя")
	void storesProfileUnderUserId() {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		profiles.saveAndFlush(Profile.of(user.getId(), "О себе", "technology", "A1"));

		//Assert
		assertThat(profiles.findById(user.getId())).isPresent();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что у профиля сохраняются все поля онбординга")
	void keepsOnboardingFields() {
		//Arrange
		AppUser user = TestFixtures.user(users);
		profiles.saveAndFlush(Profile.of(user.getId(), "Люблю спорт", "sport, travel", "B1"));

		//Act
		Profile stored = profiles.findById(user.getId()).orElseThrow();

		//Assert
		assertThat(stored.getAbout()).isEqualTo("Люблю спорт");
		assertThat(stored.getInterests()).isEqualTo("sport, travel");
		assertThat(stored.getLevel()).isEqualTo("B1");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что профиль редактируется")
	void updatesProfileFields() {
		//Arrange
		AppUser user = TestFixtures.userWithProfile(users, profiles);

		//Act
		Profile stored = profiles.findById(user.getId()).orElseThrow();
		stored.setLevel("A2");
		stored.setAbout("Теперь про книги");
		profiles.saveAndFlush(stored);

		//Assert
		assertThat(profiles.findById(user.getId()).orElseThrow().getLevel()).isEqualTo("A2");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что у пользователя без профиля ничего не находится")
	void findsNothingForUserWithoutProfile() {
		//Arrange
		AppUser user = TestFixtures.user(users);

		//Act
		boolean present = profiles.findById(user.getId()).isPresent();

		//Assert
		assertThat(present).isFalse();
	}

}
