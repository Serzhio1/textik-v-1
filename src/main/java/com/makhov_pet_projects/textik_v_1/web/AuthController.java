package com.makhov_pet_projects.textik_v_1.web;

import com.makhov_pet_projects.textik_v_1.domain.AppUser;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.EmailAlreadyTakenException;
import com.makhov_pet_projects.textik_v_1.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {

	private final UserRegistrationService registration;
	private final UserDetailsService userDetails;
	private final SecurityContextRepository securityContextRepository;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	@GetMapping("/register")
	String registerPage(Model model) {
		model.addAttribute("registrationForm", RegistrationForm.empty());
		return "register";
	}

	@PostMapping("/register")
	String register(@Valid @ModelAttribute("registrationForm") RegistrationForm form,
			BindingResult binding,
			HttpServletRequest request,
			HttpServletResponse response) {
		if (binding.hasErrors()) {
			return "register";
		}
		AppUser registered;
		try {
			registered = registration.register(form.email(), form.username(), form.password());
		} catch (EmailAlreadyTakenException e) {
			binding.rejectValue("email", "registration.emailTaken", "Такой email уже зарегистрирован");
			return "register";
		}
		logIn(registered.getEmail(), request, response);
		return "redirect:/";
	}

	@GetMapping("/login")
	String loginPage() {
		return "login";
	}

	private void logIn(String email, HttpServletRequest request, HttpServletResponse response) {
		UserDetails details = userDetails.loadUserByUsername(email);
		Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
				details, null, details.getAuthorities());
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		SecurityContextHolder.getContext().setAuthentication(authentication);
		securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
	}

}
