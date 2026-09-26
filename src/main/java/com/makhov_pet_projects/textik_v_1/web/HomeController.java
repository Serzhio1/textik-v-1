package com.makhov_pet_projects.textik_v_1.web;

import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

	@GetMapping("/")
	String home(@AuthenticationPrincipal AppUserDetails user, Model model) {
		model.addAttribute("displayName", user.displayName());
		return "home";
	}

}
