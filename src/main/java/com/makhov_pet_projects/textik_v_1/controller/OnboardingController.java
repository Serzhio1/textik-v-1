package com.makhov_pet_projects.textik_v_1.controller;

import com.makhov_pet_projects.textik_v_1.dto.ProfileForm;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

	private final ProfileService profiles;

	@GetMapping
	String showForm(@AuthenticationPrincipal AppUserDetails user, Model model) {
		if (profiles.exists(user.id())) {
			return "redirect:/progress";
		}
		addFormAttributes(model, ProfileForm.empty());
		return "onboarding";
	}

	@PostMapping
	String save(@AuthenticationPrincipal AppUserDetails user,
			@Valid @ModelAttribute("profileForm") ProfileForm form,
			BindingResult binding,
			Model model) {
		if (profiles.exists(user.id())) {
			return "redirect:/progress";
		}
		if (binding.hasErrors()) {
			addFormAttributes(model, form);
			return "onboarding";
		}
		profiles.create(user.id(), form);
		return "redirect:/";
	}

	private void addFormAttributes(Model model, ProfileForm form) {
		model.addAttribute("profileForm", form);
		model.addAttribute("levels", ProfileForm.LEVELS);
	}

}
