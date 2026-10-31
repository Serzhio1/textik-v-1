package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ProfileForm;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.SessionReview;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionReviewRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

	public static final long REVIEW_AFTER_USER_TURNS = 3;

	private final SessionRepository sessions;
	private final SessionReviewRepository reviews;
	private final AppUserRepository users;
	private final AiGateway ai;

	@Transactional(readOnly = true)
	public List<LearningSession> findHistory(Long userId) {
		return sessions.findByUserIdAndStatusOrderByIdDesc(userId, SessionStatus.DONE);
	}

	@Transactional
	public LearningSession createWithContent(Long userId, String level, GeneratedText text,
			List<GeneratedQuestion> questions) {
		AppUser user = users.findById(userId).orElseThrow();
		LearningSession session = LearningSession.proposed(user, level);
		session.setTitle(text.title());
		session.setDescription(text.description());
		session.setTextContent(text.text());
		session.advance();
		questions.forEach(question -> session.addQuestion(
				question.question(), question.options(), question.correctAnswer(), question.explanation()));
		return sessions.save(session);
	}

	@Transactional(readOnly = true)
	public Optional<LearningSession> findForOwner(Long sessionId, Long userId) {
		return sessions.findByIdAndUserId(sessionId, userId);
	}

	@Transactional
	public Optional<LearningSession> startThinking(Long sessionId, Long userId) {
		Optional<LearningSession> found = sessions.findWithQuestionsByIdAndUserId(sessionId, userId);
		found.ifPresent(LearningSession::startThinking);
		return found;
	}

	@Transactional
	public Optional<LearningSession> startDiscussion(Long sessionId, Long userId) {
		Optional<LearningSession> found = sessions.findWithChatMessagesByIdAndUserId(sessionId, userId);
		found.ifPresent(session -> {
			if (session.getStatus() == SessionStatus.THINK) {
				session.startDiscussing();
			}
			if (session.getStatus() == SessionStatus.DISCUSS && session.getChatMessages().isEmpty()) {
				tryOpenDiscussion(session);
			}
		});
		return found;
	}

	@Transactional
	public Optional<LearningSession> sendMessage(Long sessionId, Long userId, String message) {
		return sessions.findWithChatMessagesByIdAndUserId(sessionId, userId)
				.filter(session -> session.getStatus() == SessionStatus.DISCUSS)
				.map(session -> {
					String reply = ai.continueDiscussion(session, message);
					session.addUserMessage(message, correctionFor(session, message));
					session.addAssistantMessage(reply);
					return session;
				});
	}

	@Transactional
	public void answerQuestions(Long sessionId, Long userId, Map<Long, String> answers) {
		LearningSession session = sessions.findWithQuestionsByIdAndUserId(sessionId, userId).orElseThrow();
		session.getQuestions().forEach(question -> question.answer(answers.get(question.getId())));
	}

	@Transactional
	public Optional<LearningSession> openReview(Long sessionId, Long userId) {
		Optional<LearningSession> found = sessions.findWithQuestionsByIdAndUserId(sessionId, userId);
		if (found.isEmpty()) {
			return Optional.empty();
		}
		LearningSession session = found.get();
		if (session.getStatus() == SessionStatus.DISCUSS && session.getReview() != null) {
			session.finishReview();
		}
		if (session.getStatus() == SessionStatus.DISCUSS
				&& session.userTurns() >= REVIEW_AFTER_USER_TURNS
				&& session.getReview() == null) {
			saveReview(session, ai.generateReview(session));
		}
		return sessions.findWithReviewByIdAndUserId(sessionId, userId);
	}

	private void saveReview(LearningSession session, ReviewData data) {
		long correct = session.getQuestions().stream()
				.filter(question -> Boolean.TRUE.equals(question.getIsCorrect()))
				.count();
		SessionReview review = SessionReview.of(session,
				data.textSummary(),
				data.conversationSummary(),
				data.mistakes(),
				mainErrors(data),
				conversationGrade(data, session),
				session.getQuestions().size(),
				(int) correct);
		session.setReview(review);
		reviews.save(review);
		session.finishReview();
	}

	private String mainErrors(ReviewData data) {
		if (data.mainErrors() == null) {
			return "";
		}
		return data.mainErrors().stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(line -> !line.isEmpty())
				.reduce((first, second) -> first + "\n" + second)
				.orElse("");
	}

	private String conversationGrade(ReviewData data, LearningSession session) {
		String grade = data.conversationGrade() == null ? "" : data.conversationGrade().trim();
		return ProfileForm.LEVELS.contains(grade) ? grade : session.getLevel();
	}

	private String correctionFor(LearningSession session, String message) {
		try {
			return ai.correctUserMessage(session, message);
		} catch (AiGatewayException e) {
			log.warn("Не удалось получить исправление для сессии {}", session.getId(), e);
			return null;
		}
	}

	private void tryOpenDiscussion(LearningSession session) {
		try {
			session.addAssistantMessage(ai.openDiscussion(session));
		} catch (AiGatewayException e) {
			log.warn("Не удалось начать обсуждение для сессии {}", session.getId(), e);
		}
	}

}