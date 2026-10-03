package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.repository.SessionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionService {

	private final SessionRepository sessions;

	@Transactional(readOnly = true)
	public List<LearningSession> findHistory(Long userId) {
		return sessions.findByUserIdOrderByIdDesc(userId);
	}

}
