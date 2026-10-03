package com.makhov_pet_projects.textik_v_1.controller;

import com.makhov_pet_projects.textik_v_1.dto.ProfileForm;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.ProfileService;
import com.makhov_pet_projects.textik_v_1.service.SessionService;
import com.makhov_pet_projects.textik_v_1.utility.Interests;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/progress")
@RequiredArgsConstructor
public class ProfileController {

	private final ProfileService profiles;
	private final SessionService sessions;

	@GetMapping
	String show(@AuthenticationPrincipal AppUserDetails user, Model model) {
		return profiles.find(user.id())
				.map(profile -> {
					addProfileAttributes(model, user, profile);
					return "progress";
				})
				.orElse("redirect:/onboarding");
	}

	@PostMapping
	String save(@AuthenticationPrincipal AppUserDetails user,
			@Valid @ModelAttribute("profileForm") ProfileForm form,
			BindingResult binding,
			Model model) {
		if (binding.hasErrors()) {
			return profiles.find(user.id())
					.map(profile -> {
						addCommonAttributes(model, user, profile);
						return "progress";
					})
					.orElse("redirect:/onboarding");
		}
		return profiles.update(user.id(), form)
				.map(profile -> "redirect:/progress?saved")
				.orElse("redirect:/onboarding");
	}

	private void addProfileAttributes(Model model, AppUserDetails user, Profile profile) {
		addCommonAttributes(model, user, profile);
		model.addAttribute("profileForm", new ProfileForm(profile.getAbout(), profile.getInterests(), profile.getLevel()));
	}

	private void addCommonAttributes(Model model, AppUserDetails user, Profile profile) {
		List<LearningSession> history = sessions.findHistory(user.id());
		model.addAttribute("displayName", user.displayName());
		model.addAttribute("email", user.email());
		model.addAttribute("interestList", Interests.split(profile.getInterests()));
		model.addAttribute("levels", ProfileForm.LEVELS);
		model.addAttribute("history", history);
		model.addAttribute("sessionCount", history.size());
	}

}
