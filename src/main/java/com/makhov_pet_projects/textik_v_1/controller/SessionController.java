package com.makhov_pet_projects.textik_v_1.controller;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.entity.Question;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.security.AppUserDetails;
import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.ProfileService;
import com.makhov_pet_projects.textik_v_1.service.SessionService;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

	@GetMapping("/sessions/{id}/think")
	String think(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user,
			@RequestParam(defaultValue = "false") boolean error, Model model) {
		LearningSession session = sessions.startThinking(id, user.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		boolean summary = summary(session);
		long correctCount = correctCount(session);
		model.addAttribute("learningSession", session);
		model.addAttribute("questions", session.getQuestions());
		model.addAttribute("summary", summary);
		model.addAttribute("correctCount", correctCount);
		model.addAttribute("wrongCount", session.getQuestions().size() - correctCount);
		model.addAttribute("error", error);
		return "think";
	}

	@PostMapping("/sessions/{id}/think")
	String answer(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user,
			@RequestParam Map<String, String> params) {
		LearningSession session = sessions.startThinking(id, user.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (summary(session)) {
			return "redirect:/sessions/" + id + "/think";
		}
		Map<Long, String> answers = answers(session, params);
		if (answers.size() != session.getQuestions().size()) {
			return "redirect:/sessions/" + id + "/think?error=1";
		}
		sessions.answerQuestions(id, user.id(), answers);
		return "redirect:/sessions/" + id + "/think";
	}

	@GetMapping("/sessions/{id}/discuss")
	String discuss(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user,
			@RequestParam(defaultValue = "false") boolean error, Model model) {
		LearningSession session = sessions.startDiscussion(id, user.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (session.getStatus() == SessionStatus.READ) {
			return "redirect:/sessions/" + id + "/think";
		}
		addDiscussionAttributes(session, error, model);
		return "discuss";
	}

	@PostMapping("/sessions/{id}/discuss")
	String sendMessage(@PathVariable Long id, @AuthenticationPrincipal AppUserDetails user,
			@RequestParam(required = false) String message) {
		LearningSession session = sessions.findForOwner(id, user.id())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
		if (session.getStatus() != SessionStatus.DISCUSS) {
			return "redirect:/sessions/" + id + "/discuss";
		}
		if (message == null || message.isBlank()) {
			return "redirect:/sessions/" + id + "/discuss?error=1";
		}
		try {
			sessions.sendMessage(id, user.id(), message.trim());
			return "redirect:/sessions/" + id + "/discuss";
		} catch (AiGatewayException e) {
			log.warn("Не удалось получить ответ ИИ в обсуждении сессии {}", id, e);
			return "redirect:/sessions/" + id + "/discuss?error=1";
		}
	}

	private void addDiscussionAttributes(LearningSession session, boolean error, Model model) {
		long turns = session.userTurns();
		model.addAttribute("learningSession", session);
		model.addAttribute("messages", session.getChatMessages());
		model.addAttribute("userTurns", turns);
		model.addAttribute("messagesLeft", Math.max(0L, SessionService.REVIEW_AFTER_USER_TURNS - turns));
		model.addAttribute("error", error || session.getChatMessages().isEmpty());
	}

	private boolean summary(LearningSession session) {
		return !session.getQuestions().isEmpty()
				&& session.getQuestions().stream().allMatch(Question::isAnswered);
	}

	private long correctCount(LearningSession session) {
		return session.getQuestions().stream()
				.filter(question -> Boolean.TRUE.equals(question.getIsCorrect()))
				.count();
	}

	private Map<Long, String> answers(LearningSession session, Map<String, String> params) {
		Map<Long, String> answers = new HashMap<>();
		session.getQuestions().forEach(question -> {
			String answer = params.get("answer_" + question.getId());
			if (answer != null && question.getOptions().contains(answer)) {
				answers.put(question.getId(), answer);
			}
		});
		return answers;
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