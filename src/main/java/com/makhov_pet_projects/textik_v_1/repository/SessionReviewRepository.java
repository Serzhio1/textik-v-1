package com.makhov_pet_projects.textik_v_1.repository;

import com.makhov_pet_projects.textik_v_1.entity.SessionReview;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionReviewRepository extends JpaRepository<SessionReview, Long> {

	Optional<SessionReview> findBySessionId(Long sessionId);

}
