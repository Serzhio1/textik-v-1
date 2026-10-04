package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.SpringAiGateway;
import com.makhov_pet_projects.textik_v_1.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Выбор реализации слоя ИИ")
class AiGatewayWiringTests extends PostgresIntegrationTest {

	@Autowired
	private AiGateway gateway;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что без профиля local подключается реализация на Spring AI")
	void usesRemoteGatewayByDefault() {
		//Arrange
		//Act
		//Assert
		assertThat(gateway).isInstanceOf(SpringAiGateway.class);
	}

}