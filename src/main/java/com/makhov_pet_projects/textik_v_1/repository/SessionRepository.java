package com.makhov_pet_projects.textik_v_1.repository;

import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.enums.SessionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<LearningSession, Long> {

	List<LearningSession> findByUserIdOrderByIdDesc(Long userId);

	List<LearningSession> findByUserIdAndStatusOrderByIdDesc(Long userId, SessionStatus status);

	Optional<LearningSession> findByIdAndUserId(Long id, Long userId);

	long countByUserIdAndStatus(Long userId, SessionStatus status);

}
