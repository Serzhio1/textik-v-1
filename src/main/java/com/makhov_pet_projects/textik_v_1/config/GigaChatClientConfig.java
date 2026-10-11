package com.makhov_pet_projects.textik_v_1.config;

import com.makhov_pet_projects.textik_v_1.service.GigaChatTokenService;
import com.makhov_pet_projects.textik_v_1.utility.ChatCompletionIds;
import java.time.Duration;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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

	@Bean
	OpenAiHttpClientBuilderCustomizer chatCompletionIdCustomizer() {
		return builder -> builder.interceptor(chain -> {
			Response response = chain.proceed(chain.request());
			if (!chatCompletions(response.request()) || !json(response)) {
				return response;
			}
			ResponseBody peeked = response.peekBody(Long.MAX_VALUE);
			String body = peeked.string();
			String fixed = ChatCompletionIds.ensureId(body);
			if (fixed.equals(body)) {
				return response;
			}
			return response.newBuilder()
					.body(ResponseBody.create(fixed, peeked.contentType()))
					.build();
		});
	}

	private boolean chatCompletions(Request request) {
		return request.url().encodedPath().endsWith("/chat/completions");
	}

	private boolean json(Response response) {
		if (response.body() == null) {
			return false;
		}
		MediaType type = response.body().contentType();
		return type != null && "application".equals(type.type()) && "json".equals(type.subtype());
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