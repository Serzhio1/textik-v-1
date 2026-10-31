package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.DiscussionTurn;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import java.util.List;
import java.util.Map;
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
					DiscussionTurn turn = ai.continueDiscussion(session, message);
					session.addUserMessage(message, turn.correction());
					session.addAssistantMessage(turn.reply());
					return session;
				});
	}

	@Transactional
	public void answerQuestions(Long sessionId, Long userId, Map<Long, String> answers) {
		LearningSession session = sessions.findWithQuestionsByIdAndUserId(sessionId, userId).orElseThrow();
		session.getQuestions().forEach(question -> question.answer(answers.get(question.getId())));
	}

	private void tryOpenDiscussion(LearningSession session) {
		try {
			session.addAssistantMessage(ai.openDiscussion(session));
		} catch (AiGatewayException e) {
			log.warn("Не удалось начать обсуждение для сессии {}", session.getId(), e);
		}
	}

}