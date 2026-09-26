package com.makhov_pet_projects.textik_v_1.repository;

import com.makhov_pet_projects.textik_v_1.domain.Question;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	List<Question> findBySessionIdOrderById(Long sessionId);

	long countBySessionIdAndIsCorrectFalse(Long sessionId);

}
