package com.makhov_pet_projects.textik_v_1.config;

import com.makhov_pet_projects.textik_v_1.service.GigaChatTokenService;
import java.time.Duration;
import okhttp3.Request;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Подменяет статический api-key на короткоживущий access token GigaChat.
 * Модель собирает автоконфигурация Spring AI, поэтому здесь только HTTP-клиент для OAuth.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = AiProperties.PROVIDER_GIGACHAT)
@EnableConfigurationProperties(GigaChatProperties.class)
public class GigaChatClientConfig {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);

	@Bean
	GigaChatTokenService gigaChatTokenService(GigaChatProperties properties) {
		return new GigaChatTokenService(properties, restClient());
	}

	@Bean
	OpenAiHttpClientBuilderCustomizer gigaChatAuthorizationCustomizer(GigaChatTokenService tokenService) {
		return builder -> builder.interceptor(chain -> chain.proceed(authorized(chain.request(), tokenService)));
	}

	private Request authorized(Request request, GigaChatTokenService tokenService) {
		try {
			return request.newBuilder()
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.currentToken())
					.build();
		} catch (RuntimeException e) {
			throw new IllegalStateException("Не удалось авторизовать запрос в GigaChat", e);
		}
	}

	private RestClient restClient() {
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
		requestFactory.setReadTimeout(TIMEOUT);
		return RestClient.builder().requestFactory(requestFactory).build();
	}

}