package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.StubAiGateway;
import com.makhov_pet_projects.textik_v_1.support.PostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"test", "local"})
@DisplayName("Офлайн-режим слоя ИИ")
class LocalAiGatewayWiringTests extends PostgresIntegrationTest {

	@Autowired
	private AiGateway gateway;

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в профиле local подключается заглушка без обращения к сети")
	void usesStubGatewayInLocalProfile() {
		//Arrange
		//Act
		//Assert
		assertThat(gateway).isInstanceOf(StubAiGateway.class);
	}

}