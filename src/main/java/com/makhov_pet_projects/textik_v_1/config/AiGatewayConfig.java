package com.makhov_pet_projects.textik_v_1.config;

import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.FallbackAiGateway;
import com.makhov_pet_projects.textik_v_1.service.SpringAiGateway;
import com.makhov_pet_projects.textik_v_1.service.StubAiGateway;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiGatewayConfig {

	/**
	 * Один бин вместо @Profile-вариантов: профиль local и app.ai.provider=stub
	 * не должны заводить два AiGateway одновременно.
	 */
	@Bean
	AiGateway aiGateway(AiProperties properties, Environment environment,
			ObjectProvider<ChatClient.Builder> chatClientBuilder) {
		if (properties.stub() || environment.acceptsProfiles(Profiles.of("local"))) {
			return new StubAiGateway();
		}
		AiGateway primary = new SpringAiGateway(chatClientBuilder.getObject());
		return properties.stubFallback() ? new FallbackAiGateway(primary, new StubAiGateway()) : primary;
	}

}