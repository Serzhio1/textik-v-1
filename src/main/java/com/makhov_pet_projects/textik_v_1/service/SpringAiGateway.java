package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ProfileForm;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.ChatMessage;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.entity.Question;
import com.makhov_pet_projects.textik_v_1.enums.ChatRole;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import com.makhov_pet_projects.textik_v_1.utility.AiPrompts;
import com.makhov_pet_projects.textik_v_1.utility.ChatCompletionJson;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.core.ParameterizedTypeReference;

@Slf4j
@RequiredArgsConstructor
public class SpringAiGateway implements AiGateway {

	private static final int STRUCTURED_ATTEMPTS = 2;

	private static final String STRICT_JSON_RULES = """
			\n\nДополнительные правила ответа (обязательные):
			- Верни только JSON: без кода-блоков ```json, без слов и пояснений до или после.
			- Внутри строк JSON нельзя нажимать реальный Enter: каждый перенос строки записывается как два символа \\n.
			- Ответ должен разбираться строгим JSON-парсером без ошибок.""";

	private final ChatClient.Builder chatClientBuilder;

	@Override
	public List<TopicProposal> generateTopics(Profile profile) {
		StructuredOutputConverter<List<TopicProposal>> converter = listOfTopics();
		String user = String.format(AiPrompts.TOPICS_USER,
				profile.getLevel(), profile.getAbout(), profile.getInterests(), profile.getLevel());
		return askStructured(converter, AiPrompts.TOPICS_SYSTEM, user);
	}

	@Override
	public GeneratedText generateText(TopicProposal topic, Profile profile) {
		StructuredOutputConverter<GeneratedText> converter =
				new BeanOutputConverter<>(GeneratedText.class);
		String user = String.format(AiPrompts.TEXT_USER,
				topic.title(), topic.description(), profile.getLevel(), profile.getLevel(), profile.getInterests());
		return askStructured(converter, AiPrompts.TEXT_SYSTEM, user);
	}

	@Override
	public List<GeneratedQuestion> generateQuestions(GeneratedText text, Profile profile) {
		StructuredOutputConverter<List<GeneratedQuestion>> converter = listOfQuestions();
		String user = String.format(AiPrompts.QUESTIONS_USER, profile.getLevel(), text.text());
		return askStructured(converter, AiPrompts.QUESTIONS_SYSTEM, user);
	}

	@Override
	public String continueDiscussion(LearningSession session, String userMessage) {
		String user = String.format(AiPrompts.DISCUSSION_USER,
				session.getTitle(), session.getDescription(), session.getLevel(),
				session.getTextContent(), conversation(session), userMessage);
		return ask(AiPrompts.DISCUSSION_SYSTEM, user).trim();
	}

	@Override
	public ReviewData generateReview(LearningSession session) {
		StructuredOutputConverter<ReviewData> converter = new BeanOutputConverter<>(ReviewData.class);
		String user = String.format(AiPrompts.REVIEW_USER,
				session.getTitle(), session.getDescription(), session.getLevel(),
				session.getTextContent(), answers(session), conversation(session), levels());
		return askStructured(converter, AiPrompts.REVIEW_SYSTEM, user);
	}

	private StructuredOutputConverter<List<TopicProposal>> listOfTopics() {
		return new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
		});
	}

	private StructuredOutputConverter<List<GeneratedQuestion>> listOfQuestions() {
		return new BeanOutputConverter<>(new ParameterizedTypeReference<>() {
		});
	}

	private <T> T askStructured(StructuredOutputConverter<T> converter, String system, String user) {
		String prompt = user + "\n\n" + converter.getFormat() + STRICT_JSON_RULES;
		int attempt = 0;
		while (true) {
			String answer = ask(system, prompt);
			try {
				return convert(converter, answer);
			} catch (AiGatewayException parseFailure) {
				if (++attempt >= STRUCTURED_ATTEMPTS) {
					throw parseFailure;
				}
				log.warn("Ответ модели не удалось разобрать (попытка {} из {}), повторяю запрос: {}",
						attempt, STRUCTURED_ATTEMPTS, parseFailure.getMessage());
			}
		}
	}

	private String ask(String system, String user) {
		try {
			String answer = chatClientBuilder.build()
					.prompt()
					.system(system)
					.user(user)
					.call()
					.content();
			if (answer == null || answer.isBlank()) {
				log.warn("Модель вернула пустой ответ");
				throw new AiGatewayException("Модель вернула пустой ответ");
			}
			return answer;
		} catch (AiGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			log.warn("Запрос к модели не удался: {}", e.getMessage(), e);
			throw new AiGatewayException("Не удалось получить ответ модели", e);
		}
	}

	private <T> T convert(StructuredOutputConverter<T> converter, String answer) {
		try {
			return converter.convert(ChatCompletionJson.sanitize(answer));
		} catch (RuntimeException e) {
			log.warn("Не удалось разобрать ответ модели: {}", e.getMessage(), e);
			log.warn("Текст ответа модели: {}", preview(answer));
			throw new AiGatewayException("Не удалось разобрать ответ модели", e);
		}
	}

	private String preview(String answer) {
		if (answer.length() <= 2000) {
			return answer;
		}
		return answer.substring(0, 1990) + "… [обрезано, всего " + answer.length() + " символов]";
	}

	private String conversation(LearningSession session) {
		return session.getChatMessages().stream()
				.map(message -> "%s: %s".formatted(role(message), message.getContent()))
				.collect(Collectors.joining("\n"));
	}

	private String answers(LearningSession session) {
		return session.getQuestions().stream()
				.map(this::answer)
				.collect(Collectors.joining("\n"));
	}

	private String answer(Question question) {
		return "- %s\n  правильный ответ: %s\n  ответ ученика: %s (%s)".formatted(
				question.getPrompt(),
				question.getCorrectAnswer(),
				question.isAnswered() ? question.getUserAnswer() : "без ответа",
				Boolean.TRUE.equals(question.getIsCorrect()) ? "верно" : "неверно");
	}

	private String role(ChatMessage message) {
		return message.getRole() == ChatRole.USER ? "Ученик" : "Собеседник";
	}

	private String levels() {
		return String.join(", ", ProfileForm.LEVELS);
	}

}