package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Настройки GigaChat")
class GigaChatPropertiesTests {

	@Test
	@DisplayName("Адрес выдачи токена и scope берём из документов GigaChat")
	void appliesDefaults() {
		//Arrange
		//Act
		GigaChatProperties properties = new GigaChatProperties(null, null, null);

		//Assert
		assertThat(properties.authUrl()).isEqualTo("https://ngw.devices.sberbank.ru:9443/api/v2/oauth");
		assertThat(properties.scope()).isEqualTo("GIGACHAT_API_PERS");
		assertThat(properties.authKey()).isEmpty();
		assertThat(properties.configured()).isFalse();
	}

	@Test
	@DisplayName("Пробелы вокруг ключа не мешают его увидеть")
	void trimsAuthorizationKey() {
		//Arrange
		//Act
		GigaChatProperties properties = new GigaChatProperties("  key  ", "  ", "");

		//Assert
		assertThat(properties.authKey()).isEqualTo("key");
		assertThat(properties.configured()).isTrue();
	}

}