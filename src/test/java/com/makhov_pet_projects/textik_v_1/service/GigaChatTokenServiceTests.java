package com.makhov_pet_projects.textik_v_1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.makhov_pet_projects.textik_v_1.config.GigaChatProperties;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Обмен ключа авторизации GigaChat на access token")
class GigaChatTokenServiceTests {

	private static final String AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
	private static final String AUTH_KEY = "authorization-key-value";
	private static final long THIRTY_MINUTES = 30 * 60 * 1000L;

	private final MutableClock clock = new MutableClock();
	private final RestClient.Builder restClientBuilder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

	@Test
	@DisplayName("Ключ уходит в Basic-заголовке как есть, вместе с RqUID и scope")
	void exchangesAuthorizationKeyForAccessToken() {
		//Arrange
		server.expect(requestTo(AUTH_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Basic " + AUTH_KEY))
				.andExpect(header("RqUID", matchesPattern("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string("scope=GIGACHAT_API_PERS"))
				.andRespond(withSuccess(token("token-1", clock.millis() + THIRTY_MINUTES), MediaType.APPLICATION_JSON));
		GigaChatTokenService service = service(AUTH_KEY);

		//Act
		String token = service.currentToken();

		//Assert
		assertThat(token).isEqualTo("token-1");
		server.verify();
	}

	@Test
	@DisplayName("Токен переиспользуется, пока не истёк срок")
	void reusesCachedToken() {
		//Arrange
		server.expect(requestTo(AUTH_URL))
				.andRespond(withSuccess(token("token-1", clock.millis() + THIRTY_MINUTES), MediaType.APPLICATION_JSON));
		GigaChatTokenService service = service(AUTH_KEY);

		//Act
		String first = service.currentToken();
		clock.advance(Duration.ofMinutes(10));
		String second = service.currentToken();

		//Assert
		assertThat(first).isEqualTo("token-1");
		assertThat(second).isEqualTo("token-1");
		server.verify();
	}

	@Test
	@DisplayName("За минуту до истечения токен обновляется, а не используется")
	void refreshesTokenCloseToExpiry() {
		//Arrange
		server.expect(requestTo(AUTH_URL))
				.andRespond(withSuccess(token("token-1", clock.millis() + 30_000L), MediaType.APPLICATION_JSON));
		server.expect(requestTo(AUTH_URL))
				.andRespond(withSuccess(token("token-2", clock.millis() + THIRTY_MINUTES), MediaType.APPLICATION_JSON));
		GigaChatTokenService service = service(AUTH_KEY);

		//Act
		String first = service.currentToken();
		clock.advance(Duration.ofSeconds(1));
		String second = service.currentToken();

		//Assert
		assertThat(first).isEqualTo("token-1");
		assertThat(second).isEqualTo("token-2");
		server.verify();
	}

	@Test
	@DisplayName("Параллельные вызовы приводят к одному обмену токена")
	void refreshesOnceForConcurrentCallers() throws InterruptedException {
		//Arrange
		server.expect(requestTo(AUTH_URL))
				.andRespond(withSuccess(token("token-1", clock.millis() + THIRTY_MINUTES), MediaType.APPLICATION_JSON));
		GigaChatTokenService service = service(AUTH_KEY);
		int callers = 8;
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch finished = new CountDownLatch(callers);
		ExecutorService pool = Executors.newFixedThreadPool(callers);
		for (int i = 0; i < callers; i++) {
			pool.submit(() -> {
				try {
					start.await();
					assertThat(service.currentToken()).isEqualTo("token-1");
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} finally {
					finished.countDown();
				}
			});
		}

		//Act
		start.countDown();

		//Assert
		assertThat(finished.await(10, TimeUnit.SECONDS)).isTrue();
		pool.shutdown();
		server.verify();
	}

	@Test
	@DisplayName("Без ключа авторизации падаем до обращения к сети")
	void failsWhenAuthorizationKeyMissing() {
		//Arrange
		GigaChatTokenService service = service("   ");

		//Act
		//Assert
		assertThatThrownBy(service::currentToken)
				.isInstanceOf(AiGatewayException.class)
				.hasMessageContaining("GIGACHAT_AUTH_KEY");
		server.verify();
	}

	@Test
	@DisplayName("Ответ без access token считаем ошибкой")
	void failsWhenAccessTokenMissing() {
		//Arrange
		server.expect(requestTo(AUTH_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
		GigaChatTokenService service = service(AUTH_KEY);

		//Act
		//Assert
		assertThatThrownBy(service::currentToken).isInstanceOf(AiGatewayException.class);
		server.verify();
	}

	@Test
	@DisplayName("Ошибка сервера на выдаче токена пробрасывается как AiGatewayException")
	void failsWhenTokenEndpointBreaks() {
		//Arrange
		server.expect(requestTo(AUTH_URL)).andRespond(withServerError());
		GigaChatTokenService service = service(AUTH_KEY);

		//Act
		//Assert
		assertThatThrownBy(service::currentToken)
				.isInstanceOf(AiGatewayException.class)
				.hasMessageContaining("access token");
		server.verify();
	}

	private GigaChatTokenService service(String authKey) {
		GigaChatProperties properties = new GigaChatProperties(authKey, AUTH_URL, GigaChatProperties.DEFAULT_SCOPE);
		return new GigaChatTokenService(properties, restClientBuilder.build(), clock);
	}

	private String token(String value, long expiresAt) {
		return "{\"access_token\":\"%s\",\"expires_at\":%d}".formatted(value, expiresAt);
	}

	private static final class MutableClock extends Clock {

		private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

		void advance(Duration duration) {
			instant = instant.plus(duration);
		}

		@Override
		public ZoneId getZone() {
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(ZoneId zone) {
			return this;
		}

		@Override
		public Instant instant() {
			return instant;
		}

	}

}