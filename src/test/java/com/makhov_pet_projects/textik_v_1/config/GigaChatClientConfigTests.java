package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.makhov_pet_projects.textik_v_1.service.GigaChatTokenService;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.openai.http.okhttp.OpenAiHttpClientBuilderCustomizer;
import org.springframework.ai.openai.http.okhttp.SpringAiOpenAiHttpClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Подключение GigaChat")
class GigaChatClientConfigTests {

	private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(GigaChatClientConfig.class);

	@Test
	@DisplayName("Для GigaChat заводим сервис токена и перехватчик заголовка авторизации")
	void registersTokenServiceAndAuthorizationInterceptor() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=gigachat", "app.ai.gigachat.auth-key=key").run(context -> {
			//Assert
			assertThat(context).hasSingleBean(GigaChatTokenService.class);
			assertThat(context).hasSingleBean(OpenAiHttpClientBuilderCustomizer.class);
			assertThat(interceptorOf(context.getBean(OpenAiHttpClientBuilderCustomizer.class))).isNotNull();
		});
	}

	@Test
	@DisplayName("Для остальных провайдеров перехватчик не добавляется")
	void backsOffForOtherProviders() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=openai", "app.ai.gigachat.auth-key=key").run(context -> {
			//Assert
			assertThat(context).doesNotHaveBean(GigaChatTokenService.class);
			assertThat(context).doesNotHaveBean(OpenAiHttpClientBuilderCustomizer.class);
		});
	}

	@Test
	@DisplayName("Статический api-key заменяется на Bearer со свежим access token")
	void replacesStaticApiKeyWithAccessToken() throws IOException {
		//Arrange
		RestClient.Builder restClientBuilder = RestClient.builder();
		MockRestServiceServer tokenServer = MockRestServiceServer.bindTo(restClientBuilder).build();
		long expiresAt = System.currentTimeMillis() + 30 * 60 * 1000L;
		tokenServer.expect(requestTo(AUTH_URL))
				.andRespond(withSuccess("{\"access_token\":\"live-token\",\"expires_at\":%d}".formatted(expiresAt),
						MediaType.APPLICATION_JSON));
		GigaChatTokenService tokenService = new GigaChatTokenService(
				new GigaChatProperties("key", AUTH_URL, GigaChatProperties.DEFAULT_SCOPE),
				restClientBuilder.build());
		Interceptor interceptor = interceptorOf(
				new GigaChatClientConfig().gigaChatAuthorizationCustomizer(tokenService));

		//Act
		Request authorized = captureAuthorizedRequest(interceptor);

		//Assert
		assertThat(authorized.header("Authorization")).isEqualTo("Bearer live-token");
		tokenServer.verify();
	}

	private Interceptor interceptorOf(OpenAiHttpClientBuilderCustomizer customizer) {
		SpringAiOpenAiHttpClient.Builder builder = SpringAiOpenAiHttpClient.builder();
		customizer.customize(builder);
		List<Interceptor> interceptors = builder.build().getOkHttpClient().interceptors();
		// Наш перехватчик добавляется последним, перед ним — перехватчик наблюдений Micrometer.
		return interceptors.get(interceptors.size() - 1);
	}

	private Request captureAuthorizedRequest(Interceptor interceptor) throws IOException {
		AtomicReference<Request> captured = new AtomicReference<>();
		Interceptor.Chain chain = (Interceptor.Chain) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				new Class<?>[] {Interceptor.Chain.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "request" -> new Request.Builder().url("https://api.giga.chat/v1/chat/completions").build();
					case "proceed" -> {
						captured.set((Request) args[0]);
						yield new Response.Builder()
								.request((Request) args[0])
								.protocol(Protocol.HTTP_1_1)
								.code(200)
								.message("OK")
								.build();
					}
					default -> throw new UnsupportedOperationException(method.getName());
				});
		interceptor.intercept(chain);
		return captured.get();
	}

}