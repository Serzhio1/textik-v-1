package com.makhov_pet_projects.textik_v_1.controller;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.ProfileService;
import com.makhov_pet_projects.textik_v_1.service.SessionService;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SessionController {

	private static final String TOPICS_SESSION_ATTR = "topicProposals";

	private final AiGateway ai;
	private final ProfileService profiles;
	private final SessionService sessions;

	@GetMapping("/select")
	String select(@AuthenticationPrincipal AppUserDetails user, HttpSession httpSession, Model model) {
		Profile profile = profiles.find(user.id()).orElseThrow();
		List<TopicProposal> topics = ai.generateTopics(profile);
		httpSession.setAttribute(TOPICS_SESSION_ATTR, topics);
		model.addAttribute("topics", topics);
		return "select";
	}

	@PostMapping("/sessions")
	String create(@AuthenticationPrincipal AppUserDetails user, @RequestParam int topic, HttpSession httpSession) {
		List<TopicProposal> proposals = proposals(httpSession);
		if (invalid(proposals, topic)) {
			return "redirect:/select";
		}
		Profile profile = profiles.find(user.id()).orElseThrow();
		try {
			GeneratedText text = ai.generateText(proposals.get(topic), profile);
			List<GeneratedQuestion> questions = ai.generateQuestions(text, profile);
			LearningSession session = sessions.createWithContent(user.id(), profile.getLevel(), text, questions);
			return "redirect:/sessions/" + session.getId() + "/read";
		} catch (AiGatewayException e) {
			log.warn("Не удалось подготовить материалы сессии, перенаправляем на /select?error=1", e);
			return "redirect:/select?error=1";
		}
	}

	@GetMapping("/sessions/{id}/read")
	String read(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user, Model model) {
		LearningSession session = sessions.findForOwner(id, user.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		model.addAttribute("learningSession", session);
		return "read";
	}

	private List<TopicProposal> proposals(HttpSession httpSession) {
		Object attribute = httpSession.getAttribute(TOPICS_SESSION_ATTR);
		return attribute instanceof List<?> list ? list.stream()
				.filter(TopicProposal.class::isInstance)
				.map(TopicProposal.class::cast)
				.toList()
				: List.of();
	}

	private boolean invalid(List<TopicProposal> proposals, int topic) {
		return topic < 0 || topic >= proposals.size();
	}

}