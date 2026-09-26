package com.makhov_pet_projects.textik_v_1.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ChatMessage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	@ToString.Exclude
	private LearningSession session;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 20)
	private ChatRole role;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	static ChatMessage fromUser(LearningSession session, String content) {
		return new ChatMessage(session, ChatRole.USER, content);
	}

	static ChatMessage fromAssistant(LearningSession session, String content) {
		return new ChatMessage(session, ChatRole.ASSISTANT, content);
	}

	private ChatMessage(LearningSession session, ChatRole role, String content) {
		this.session = session;
		this.role = role;
		this.content = content;
	}

}
