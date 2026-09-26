package com.makhov_pet_projects.textik_v_1.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "session_reviews")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SessionReview {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	@EqualsAndHashCode.Include
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false, unique = true)
	@ToString.Exclude
	private LearningSession session;

	@Column(name = "text_summary", columnDefinition = "text")
	private String textSummary;

	@Column(name = "conversation_summary", columnDefinition = "text")
	private String conversationSummary;

	@Column(name = "mistakes", columnDefinition = "text")
	private String mistakes;

	@Column(name = "conversation_grade", length = 2)
	private String conversationGrade;

	@Column(name = "questions_total", nullable = false)
	private int questionsTotal;

	@Column(name = "questions_correct", nullable = false)
	private int questionsCorrect;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public static SessionReview of(LearningSession session, String textSummary, String conversationSummary,
			String mistakes, String conversationGrade, int questionsTotal, int questionsCorrect) {
		SessionReview review = new SessionReview();
		review.setSession(session);
		review.setTextSummary(textSummary);
		review.setConversationSummary(conversationSummary);
		review.setMistakes(mistakes);
		review.setConversationGrade(conversationGrade);
		review.setQuestionsTotal(questionsTotal);
		review.setQuestionsCorrect(questionsCorrect);
		return review;
	}

}
