package com.makhov_pet_projects.textik_v_1.config;

import com.makhov_pet_projects.textik_v_1.security.OnboardingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final OnboardingInterceptor onboardingInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(onboardingInterceptor)
				.excludePathPatterns("/onboarding", "/logout", "/login", "/register", "/error", "/css/**");
	}

}
