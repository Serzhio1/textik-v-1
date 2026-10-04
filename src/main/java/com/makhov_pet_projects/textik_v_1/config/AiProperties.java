package com.makhov_pet_projects.textik_v_1.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String fallback) {

	public static final String FALLBACK_NONE = "none";
	public static final String FALLBACK_STUB = "stub";

	public AiProperties {
		if (fallback == null || fallback.isBlank()) {
			fallback = FALLBACK_NONE;
		}
		fallback = fallback.trim().toLowerCase();
	}

	public boolean stubFallback() {
		return FALLBACK_STUB.equals(fallback);
	}

}