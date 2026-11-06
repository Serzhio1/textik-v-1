package com.makhov_pet_projects.textik_v_1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.AppUser;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.support.FakeChatModel;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;

@DisplayName("Слой ИИ на Spring AI")
class SpringAiGatewayTests {

	private static final String TOPICS_JSON = """
			[
			  {"title": "Reading for Progress", "description": "О чём текст"},
			  {"title": "Small Habits", "description": "О чём второй"},
			  {"title": "Notetaking", "description": "О чём третий"}
			]""";

	private static final String TEXT_JSON = """
			{"title": "Reading for Progress", "description": "О чём текст", "text": "Reading is useful."}""";

	private static final String QUESTIONS_JSON = """
			[
			  {"question": "О чём текст?", "options": ["A", "B", "C", "D"],
			   "correctAnswer": "B", "explanation": "Потому что так написано"}
			]""";

	private static final String REVIEW_JSON = """
			{"textSummary": "Резюме текста", "conversationSummary": "Резюме разговора",
			 "mistakes": "Ошибки ученика", "conversationGrade": "A2",
			 "mainErrors": ["Первая ошибка", "Вторая ошибка"]}""";

	private static final String DISCUSSION_REPLY = "  Which part did you like most?  ";

	private static final String CORRECTION_JSON = """
			{"correctedLearnerMessage": "  I have a question.  "}""";

	private static final String EMPTY_CORRECTION_JSON = """
			{"correctedLearnerMessage": "   "}""";

	private static final String CLARIFICATION_CORRECTION_JSON = """
			{"correctedLearnerMessage": "Sure, I'd love to clarify my question. What factors matter most to you?"}""";

	private static final String EXPANDED_CORRECTION_JSON = """
			{"correctedLearnerMessage": "Can you ask me another question about the benefits mentioned in the text?"}""";

	private final Profile profile = Profile.of(1L, "Люблю IT", "технологии, спорт", "B1");

	@Test
	@DisplayName("Тест, в котором мы проверяем, что темы разбираются из JSON-ответа модели")
	void parsesTopics() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(TOPICS_JSON);

		//Act
		List<TopicProposal> topics = gateway.generateTopics(profile);

		//Assert
		assertThat(topics).hasSize(3);
		assertThat(topics.getFirst().title()).isEqualTo("Reading for Progress");
		assertThat(topics.getFirst().description()).isEqualTo("О чём текст");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что текст разбирается из JSON-ответа модели")
	void parsesText() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(TEXT_JSON);

		//Act
		GeneratedText text = gateway.generateText(new TopicProposal("Reading", "О чём"), profile);

		//Assert
		assertThat(text.title()).isEqualTo("Reading for Progress");
		assertThat(text.text()).isEqualTo("Reading is useful.");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что промпт текста просит поля DTO, а не выдуманную схему")
	void sendsTextPromptWithDtoFields() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, TEXT_JSON)));

		//Act
		gateway.generateText(new TopicProposal("Reading", "О чём"), profile);

		//Assert
		String prompt = recorded.get().getUserMessage().getText();
		assertThat(prompt)
				.contains("B1")
				.contains("технологии, спорт")
				.contains("\"text\"")
				.doesNotContain("\"paragraphs\"")
				.doesNotContain("\"introduction\"")
				.doesNotContain("audio");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что вопросы разбираются вместе с вариантами ответа")
	void parsesQuestions() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(QUESTIONS_JSON);

		//Act
		List<GeneratedQuestion> questions =
				gateway.generateQuestions(new GeneratedText("T", "D", "Reading is useful."), profile);

		//Assert
		assertThat(questions).hasSize(1);
		assertThat(questions.getFirst().options()).containsExactly("A", "B", "C", "D");
		assertThat(questions.getFirst().correctAnswer()).isEqualTo("B");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что итоги сессии разбираются из JSON-ответа модели")
	void parsesReview() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(REVIEW_JSON);

		//Act
		ReviewData review = gateway.generateReview(session());

		//Assert
		assertThat(review.conversationGrade()).isEqualTo("A2");
		assertThat(review.mainErrors()).containsExactly("Первая ошибка", "Вторая ошибка");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ собеседника приходит строкой без пробелов по краям")
	void parsesDiscussionReply() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(DISCUSSION_REPLY);

		//Act
		String reply = gateway.continueDiscussion(session(), "Мне понравился пример про привычки");

		//Assert
		assertThat(reply).isEqualTo("Which part did you like most?");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что исправление разбирается из отдельного JSON-ответа")
	void parsesCorrection() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(CORRECTION_JSON);

		//Act
		String correction = gateway.correctUserMessage(session(), "i has a question");

		//Assert
		assertThat(correction).isEqualTo("I have a question.");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пустое исправление остаётся пустой строкой")
	void keepsEmptyCorrection() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(EMPTY_CORRECTION_JSON);

		//Act
		String correction = gateway.correctUserMessage(session(), "I have a question.");

		//Assert
		assertThat(correction).isEmpty();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ-уточнение вместо исправления отклоняется")
	void rejectsClarificationAsCorrection() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(CLARIFICATION_CORRECTION_JSON);

		//Act
		String correction = gateway.correctUserMessage(
				session(), "i dont understand your question. Doy ask this more clearly?");

		//Assert
		assertThat(correction).isNull();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что исправление с лишними идеями отклоняется")
	void rejectsExpandedCorrection() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering(EXPANDED_CORRECTION_JSON);

		//Act
		String correction = gateway.correctUserMessage(session(), "can you ask me another question?");

		//Assert
		assertThat(correction).isNull();
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ модели в блоке кода всё равно разбирается")
	void parsesAnswerWrappedInCodeBlock() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering("```json\n" + TOPICS_JSON + "\n```");

		//Act
		List<TopicProposal> topics = gateway.generateTopics(profile);

		//Assert
		assertThat(topics).hasSize(3);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неразбираемый ответ модели превращается в AiGatewayException")
	void reportsUnparseableAnswer() {
		//Arrange
		SpringAiGateway gateway = gatewayAnswering("извините, я не смог");

		//Assert
		assertThatThrownBy(() -> gateway.generateTopics(profile))
				.isInstanceOf(AiGatewayException.class)
				.hasMessageContaining("Не удалось разобрать");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что сбой endpoint превращается в AiGatewayException")
	void reportsEndpointFailure() {
		//Arrange
		SpringAiGateway gateway = new SpringAiGateway(ChatClient.builder(FakeChatModel.failing()));

		//Assert
		assertThatThrownBy(() -> gateway.generateTopics(profile))
				.isInstanceOf(AiGatewayException.class)
				.hasMessageContaining("Не удалось получить ответ модели");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в промпт уходит профиль ученика и требование вернуть JSON")
	void sendsProfileAndJsonSchema() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, TOPICS_JSON)));

		//Act
		gateway.generateTopics(profile);

		//Assert
		assertThat(recorded.get().getUserMessage().getText())
				.contains("B1")
				.contains("технологии, спорт")
				.contains("JSON");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ИИ начинает обсуждение вопросом по прочитанному тексту")
	void opensDiscussionWithQuestion() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, "  What did you think about the text?  ")));
		LearningSession session = session();

		//Act
		String opening = gateway.openDiscussion(session);

		//Assert
		assertThat(opening).isEqualTo("What did you think about the text?");
		assertThat(recorded.get().getUserMessage().getText())
				.contains("Reading is useful.")
				.contains("B1");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в промпт обсуждения уходит прочитанный текст и реплики")
	void sendsTextAndConversation() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, DISCUSSION_REPLY)));
		LearningSession session = session();
		session.addUserMessage("Мне понравился пример про привычки", null);

		//Act
		gateway.continueDiscussion(session, "А что ещё скажешь?");

		//Assert
		assertThat(recorded.get().getUserMessage().getText())
				.contains("Reading is useful.")
				.contains("Мне понравился пример про привычки")
				.contains("А что ещё скажешь?");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что в промпт исправления уходят уровень, контекст и сообщение")
	void sendsLevelContextAndUserMessageToCorrectionPrompt() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, CORRECTION_JSON)));
		LearningSession session = session();
		session.addAssistantMessage("What did you think about the text?");
		session.addAssistantMessage("Which part was most useful?");

		//Act
		gateway.correctUserMessage(session, "i has a question");

		//Assert
		assertThat(recorded.get().getUserMessage().getText())
				.contains("B1")
				.contains("Which part was most useful?")
				.doesNotContain("What did you think about the text?")
				.contains("i has a question")
				.contains("correctedLearnerMessage");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что исправление работает без предыдущей реплики")
	void correctsWithoutPreviousAssistantMessage() {
		//Arrange
		AtomicReference<Prompt> recorded = new AtomicReference<>();
		SpringAiGateway gateway = new SpringAiGateway(
				ChatClient.builder(FakeChatModel.recording(recorded, CORRECTION_JSON)));

		//Act
		String correction = gateway.correctUserMessage(session(), "i has a question");

		//Assert
		assertThat(correction).isEqualTo("I have a question.");
		assertThat(recorded.get().getUserMessage().getText()).doesNotContain("null");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что неразбираемый ответ перезапрашивается один раз")
	void retriesParseFailureOnce() {
		//Arrange
		SpringAiGateway gateway = gatewayAnsweringSequence("извините, я не смог", TOPICS_JSON);

		//Act
		List<TopicProposal> topics = gateway.generateTopics(profile);

		//Assert
		assertThat(topics).hasSize(3);
		assertThat(topics.getFirst().title()).isEqualTo("Reading for Progress");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что ответ с сырыми переносами строк чинится до разбора")
	void parsesAnswerWithRawControlCharacters() {
		//Arrange
		String messy = "```json\n[{\"title\": \"Reading for Progress\", \"description\": \"О чём текст\", \"text\": \"Line one.\nLine two\"}]\n```";
		SpringAiGateway gateway = gatewayAnswering(messy);

		//Act
		List<TopicProposal> topics = gateway.generateTopics(profile);

		//Assert
		assertThat(topics).hasSize(1);
		assertThat(topics.getFirst().title()).isEqualTo("Reading for Progress");
	}

	private SpringAiGateway gatewayAnswering(String answer) {
		return new SpringAiGateway(ChatClient.builder(FakeChatModel.answeringWith(answer)));
	}

	private SpringAiGateway gatewayAnsweringSequence(String first, String second) {
		return new SpringAiGateway(ChatClient.builder(FakeChatModel.answeringWithSequence(List.of(first, second))));
	}

	private LearningSession session() {
		LearningSession session = LearningSession.proposed(new AppUser("a@example.com", "Sergey", "hash"), "B1");
		session.setTitle("Reading for Progress");
		session.setDescription("О чём текст");
		session.setTextContent("Reading is useful.");
		return session;
	}

}