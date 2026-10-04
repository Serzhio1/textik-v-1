package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Настройки слоя ИИ")
class AiPropertiesTests {

	@Test
	@DisplayName("Без значений используем OpenAI-совместимый клиент без резервной модели")
	void appliesDefaults() {
		//Arrange
		//Act
		AiProperties properties = new AiProperties(null, null);

		//Assert
		assertThat(properties.provider()).isEqualTo(AiProperties.PROVIDER_OPENAI);
		assertThat(properties.gigachat()).isFalse();
		assertThat(properties.stub()).isFalse();
		assertThat(properties.stubFallback()).isFalse();
	}

	@Test
	@DisplayName("Регистр и пробелы не влияют на выбор провайдера")
	void normalizesValues() {
		//Arrange
		//Act
		AiProperties properties = new AiProperties("  GigaChat ", " STUB ");

		//Assert
		assertThat(properties.gigachat()).isTrue();
		assertThat(properties.stubFallback()).isTrue();
	}

	@Test
	@DisplayName("Пустое значение провайдера равносильно значению по умолчанию")
	void treatsBlankProviderAsDefault() {
		//Arrange
		//Act
		AiProperties properties = new AiProperties("   ", "none");

		//Assert
		assertThat(properties.provider()).isEqualTo(AiProperties.PROVIDER_OPENAI);
	}

	@Test
	@DisplayName("Заглушку можно выбрать прямо в конфигурации")
	void recognizesStubProvider() {
		//Arrange
		//Act
		AiProperties properties = new AiProperties(AiProperties.PROVIDER_STUB, AiProperties.FALLBACK_NONE);

		//Assert
		assertThat(properties.stub()).isTrue();
	}

}