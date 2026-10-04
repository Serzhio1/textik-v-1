package com.makhov_pet_projects.textik_v_1.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Заглушка слоя ИИ для офлайн-работы")
class StubAiGatewayTests {

	private final StubAiGateway gateway = new StubAiGateway();

	private final Profile profile = Profile.of(1L, "Люблю IT", "технологии, спорт", "A2");

	@Test
	@DisplayName("Тест, в котором мы проверяем, что заглушка подбирает три темы")
	void proposesThreeTopics() {
		//Arrange
		//Act
		List<TopicProposal> topics = gateway.generateTopics(profile);

		//Assert
		assertThat(topics).hasSize(3);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что по выбранной теме заглушка отдаёт текст нужного объёма")
	void generatesTextForTopic() {
		//Arrange
		TopicProposal topic = gateway.generateTopics(profile).getFirst();

		//Act
		GeneratedText text = gateway.generateText(topic, profile);

		//Assert
		assertThat(text.title()).isEqualTo(topic.title());
		assertThat(text.text().split("\\s+")).hasSizeGreaterThan(300);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что у каждого вопроса заглушки четыре варианта и верный среди них")
	void generatesQuestionsWithFourOptions() {
		//Arrange
		GeneratedText text = gateway.generateText(gateway.generateTopics(profile).getFirst(), profile);

		//Act
		List<GeneratedQuestion> questions = gateway.generateQuestions(text, profile);

		//Assert
		assertThat(questions).isNotEmpty();
		assertThat(questions).allSatisfy(question -> {
			assertThat(question.options()).hasSize(4);
			assertThat(question.options()).contains(question.correctAnswer());
			assertThat(question.explanation()).isNotBlank();
		});
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что заглушка меняет реплики по ходу разговора")
	void changesReplyAsConversationGrows() {
		//Arrange
		LearningSession session = session();
		session.addUserMessage("Мне понравился первый абзац");

		//Act
		String first = gateway.continueDiscussion(session, "Первый вопрос");
		session.addAssistantMessage(first);
		session.addUserMessage("Второй вопрос");
		String second = gateway.continueDiscussion(session, "Второй вопрос");

		//Assert
		assertThat(first).isNotEqualTo(second);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что итоги сессии содержат оценку и список ошибок")
	void generatesReview() {
		//Arrange
		LearningSession session = session();

		//Act
		ReviewData review = gateway.generateReview(session);

		//Assert
		assertThat(review.conversationGrade()).isEqualTo("A2");
		assertThat(review.mainErrors()).isNotEmpty();
		assertThat(review.textSummary()).isNotBlank();
	}

	private LearningSession session() {
		LearningSession session = LearningSession.proposed(new AppUser("a@example.com", "Sergey", "hash"), "A2");
		session.setTitle("Reading for Progress");
		session.setDescription("О чём текст");
		session.setTextContent("Reading is useful.");
		return session;
	}

}