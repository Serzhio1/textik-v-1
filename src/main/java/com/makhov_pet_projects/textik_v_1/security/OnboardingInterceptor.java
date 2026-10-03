package com.makhov_pet_projects.textik_v_1.security;

import com.makhov_pet_projects.textik_v_1.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class OnboardingInterceptor implements HandlerInterceptor {

	private static final String ONBOARDING_PATH = "/onboarding";

	private final ProfileService profiles;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
		AppUserDetails user = currentUser();
		if (user == null || isOnboarding(request)) {
			return true;
		}
		if (profiles.exists(user.id())) {
			return true;
		}
		response.sendRedirect(request.getContextPath() + ONBOARDING_PATH);
		return false;
	}

	private AppUserDetails currentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof AppUserDetails details
				? details
				: null;
	}

	private boolean isOnboarding(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length());
		return ONBOARDING_PATH.equals(path);
	}

}
