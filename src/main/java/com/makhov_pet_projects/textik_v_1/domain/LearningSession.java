package com.makhov_pet_projects.textik_v_1.domain;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LearningSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	@EqualsAndHashCode.Include
	@ToString.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	@ToString.Exclude
	private AppUser user;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	@ToString.Include
	private SessionStatus status = SessionStatus.PROPOSED;

	@Column(name = "level", length = 2)
	private String level;

	@Column(name = "title")
	@ToString.Include
	private String title;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "text_content", columnDefinition = "text")
	private String textContent;

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<Question> questions = new ArrayList<>();

	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private List<ChatMessage> chatMessages = new ArrayList<>();

	@OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	private SessionReview review;

	public static LearningSession proposed(AppUser user, String level) {
		LearningSession session = new LearningSession();
		session.setUser(user);
		session.setLevel(level);
		return session;
	}

	public void advance() {
		this.status = this.status.next();
	}

	public void addQuestion(String prompt, List<String> options, String correctAnswer, String explanation) {
		questions.add(Question.singleChoice(this, prompt, options, correctAnswer, explanation));
	}

	public void addUserMessage(String content) {
		chatMessages.add(ChatMessage.fromUser(this, content));
	}

	public void addAssistantMessage(String content) {
		chatMessages.add(ChatMessage.fromAssistant(this, content));
	}

	public long userTurns() {
		return chatMessages.stream().filter(message -> message.getRole() == ChatRole.USER).count();
	}

}
