package com.makhov_pet_projects.textik_v_1.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.makhov_pet_projects.textik_v_1.config.GigaChatProperties;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Обменивает Authorization key на короткоживущий access token GigaChat (30 минут)
 * и отдаёт актуальный токен. Ключ и токен нигде не логируются.
 */
public class GigaChatTokenService {

	private static final long EXPIRY_MARGIN_MILLIS = 60_000L;
	private static final long FALLBACK_LIFETIME_MILLIS = 25 * 60 * 1000L;

	private final GigaChatProperties properties;
	private final RestClient restClient;
	private final Clock clock;
	private final Lock lock = new ReentrantLock();

	private volatile String token;
	private volatile long expiresAt;

	public GigaChatTokenService(GigaChatProperties properties, RestClient restClient) {
		this(properties, restClient, Clock.systemUTC());
	}

	GigaChatTokenService(GigaChatProperties properties, RestClient restClient, Clock clock) {
		this.properties = properties;
		this.restClient = restClient;
		this.clock = clock;
	}

	public String currentToken() {
		if (usable()) {
			return token;
		}
		lock.lock();
		try {
			if (!usable()) {
				refresh();
			}
			return token;
		} finally {
			lock.unlock();
		}
	}

	private boolean usable() {
		String current = token;
		return current != null && clock.millis() < expiresAt - EXPIRY_MARGIN_MILLIS;
	}

	private void refresh() {
		if (!properties.configured()) {
			throw new AiGatewayException(
					"Не задан GIGACHAT_AUTH_KEY: получите Authorization key в проекте GigaChat API (Studio → Настройки API)");
		}
		try {
			AccessToken response = restClient.post()
					.uri(properties.authUrl())
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.header("RqUID", UUID.randomUUID().toString())
					.header(HttpHeaders.AUTHORIZATION, "Basic " + properties.authKey())
					.body(form())
					.retrieve()
					.body(AccessToken.class);
			if (response == null || response.token() == null || response.token().isBlank()) {
				throw new AiGatewayException("GigaChat не вернул access token");
			}
			token = response.token();
			expiresAt = response.expiresAt() != null && response.expiresAt() > 0
					? response.expiresAt()
					: clock.millis() + FALLBACK_LIFETIME_MILLIS;
		} catch (AiGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new AiGatewayException("Не удалось получить access token GigaChat", e);
		}
	}

	private LinkedMultiValueMap<String, String> form() {
		LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("scope", properties.scope());
		return form;
	}

	public record AccessToken(
			@JsonProperty("access_token") String token,
			@JsonProperty("expires_at") Long expiresAt) {
	}

}