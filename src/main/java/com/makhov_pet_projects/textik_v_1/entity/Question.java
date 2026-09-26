package com.makhov_pet_projects.textik_v_1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Setter(AccessLevel.NONE)
	@EqualsAndHashCode.Include
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "session_id", nullable = false)
	@ToString.Exclude
	private LearningSession session;

	@Column(name = "prompt", nullable = false, columnDefinition = "text")
	private String prompt;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "options", nullable = false)
	private List<String> options;

	@Column(name = "correct_answer", nullable = false, columnDefinition = "text")
	private String correctAnswer;

	@Column(name = "explanation", columnDefinition = "text")
	private String explanation;

	@Column(name = "user_answer", columnDefinition = "text")
	private String userAnswer;

	@Column(name = "is_correct")
	private Boolean isCorrect;

	static Question singleChoice(LearningSession session, String prompt, List<String> options,
			String correctAnswer, String explanation) {
		Question question = new Question();
		question.setSession(session);
		question.setPrompt(prompt);
		question.setOptions(List.copyOf(options));
		question.setCorrectAnswer(correctAnswer);
		question.setExplanation(explanation);
		return question;
	}

	public void answer(String answer) {
		this.userAnswer = answer;
		this.isCorrect = Objects.equals(correctAnswer, answer);
	}

	public boolean isAnswered() {
		return userAnswer != null;
	}

}
