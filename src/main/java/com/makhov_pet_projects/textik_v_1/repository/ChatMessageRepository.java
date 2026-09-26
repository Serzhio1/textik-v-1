package com.makhov_pet_projects.textik_v_1.repository;

import com.makhov_pet_projects.textik_v_1.domain.ChatMessage;
import com.makhov_pet_projects.textik_v_1.domain.ChatRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findBySessionIdOrderById(Long sessionId);

	long countBySessionIdAndRole(Long sessionId, ChatRole role);

}
