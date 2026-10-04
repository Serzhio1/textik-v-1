package com.makhov_pet_projects.textik_v_1.config;

import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.FallbackAiGateway;
import com.makhov_pet_projects.textik_v_1.service.SpringAiGateway;
import com.makhov_pet_projects.textik_v_1.service.StubAiGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiGatewayConfig {

	@Bean
	@Profile("local")
	AiGateway localAiGateway() {
		return new StubAiGateway();
	}

	@Bean
	@Profile("!local")
	AiGateway remoteAiGateway(AiProperties properties, ChatClient.Builder chatClientBuilder) {
		AiGateway primary = new SpringAiGateway(chatClientBuilder);
		return properties.stubFallback() ? new FallbackAiGateway(primary, new StubAiGateway()) : primary;
	}

}