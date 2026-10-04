package com.makhov_pet_projects.textik_v_1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.gigachat")
public record GigaChatProperties(String authKey, String authUrl, String scope) {

	public static final String DEFAULT_AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
	public static final String DEFAULT_SCOPE = "GIGACHAT_API_PERS";

	public GigaChatProperties {
		authKey = authKey == null ? "" : authKey.trim();
		authUrl = blank(authUrl) ? DEFAULT_AUTH_URL : authUrl.trim();
		scope = blank(scope) ? DEFAULT_SCOPE : scope.trim();
	}

	public boolean configured() {
		return !authKey.isBlank();
	}

	private static boolean blank(String value) {
		return value == null || value.isBlank();
	}

}