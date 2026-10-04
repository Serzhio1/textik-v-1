package com.makhov_pet_projects.textik_v_1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, String fallback) {

	public static final String PROVIDER_OPENAI = "openai";
	public static final String PROVIDER_GIGACHAT = "gigachat";
	public static final String PROVIDER_STUB = "stub";

	public static final String FALLBACK_NONE = "none";
	public static final String FALLBACK_STUB = "stub";

	public AiProperties {
		provider = normalize(provider, PROVIDER_OPENAI);
		fallback = normalize(fallback, FALLBACK_NONE);
	}

	public boolean gigachat() {
		return PROVIDER_GIGACHAT.equals(provider);
	}

	public boolean stub() {
		return PROVIDER_STUB.equals(provider);
	}

	public boolean stubFallback() {
		return FALLBACK_STUB.equals(fallback);
	}

	private static String normalize(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value.trim().toLowerCase();
	}

}