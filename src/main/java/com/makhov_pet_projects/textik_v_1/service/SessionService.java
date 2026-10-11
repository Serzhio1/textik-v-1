package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import com.makhov_pet_projects.textik_v_1.repository.AppUserRepository;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

	private final SessionRepository sessions;
	private final AppUserRepository users;

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

}