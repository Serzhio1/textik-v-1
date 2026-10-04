package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import java.util.List;

public class StubAiGateway implements AiGateway {

	private static final List<TopicProposal> TOPICS = List.of(
			new TopicProposal("Reading for Progress", "Почему ежедневное чтение полезнее, чем разовые занятия"),
			new TopicProposal("Notetaking That Works", "Как записывать мысли так, чтобы к ним возвращаться"),
			new TopicProposal("Small Habits, Big Results", "Почему маленькие ежедневные привычки дают больше, чем редкие подвиги"));

	private static final String TEXT = """
			Reading is one of the most reliable ways to improve your English skills. When you read \
			regulary, you meet new words in context, see grammar used naturally, and gradually start \
			to feel how ideas connect with each other. The important part is to choose a text that \
			matches your level. If a text is too difficult, you stop enjoying it. If it is too easy, \
			you make almost no progress. The sweet spot is a text where you understand most of the \
			words and still meet a few new ones.

			Many learners try to learn vocabulary from lists alone. This method works slowly, because a \
			word on a list is easy to forget and has no story around it. When the same word appears in \
			a real article, it stays in your memory much longer. Reading also teaches collocations, \
			which are words that naturally go together. Native speakers say "make a decision", not \
			"do a decision". Such patterns are hard to notice in a dictionary, but they become clear \
			while you read.

			Reading also improves writing. The more you read, the more sentence patterns and \
			transitions you absorb without noticing. After a few months of regular reading, your own \
			writing sounds more natural. Your attention span grows too, and you start thinking in \
			English instead of translating every phrase in your head.

			To get the most from a text, read actively. Do not stop at every unknown word — try to \
			guess it from the context first, and only then check the ones that matter. After a \
			section, say out loud what it was about in your own words. This small exercise checks your \
			understanding and keeps your attention awake.

			In the end, consistency beats intensity. Reading for fifteen minutes a day gives far \
			better results than reading for three hours once a month. Pick topics you actually enjoy, \
			and reading stops feeling like homework. Over time this simple habit lifts every other \
			part of your English as well.
			""";

	private static final List<GeneratedQuestion> QUESTIONS = List.of(
			new GeneratedQuestion(
					"Почему автор советует выбирать текст по уровню?",
					List.of("Чтобы читать было быстрее",
							"Чтобы текст был слишком лёгким",
							"Чтобы не бросить чтение и получать новые слова",
							"Чтобы тратить меньше времени"),
					"Чтобы не бросить чтение и получать новые слова",
					"Слишком сложный текст заставляет бросить чтение, а слишком лёгкий не даёт прогресса. Нужен баланс."),
			new GeneratedQuestion(
					"Что такое collocations в тексте?",
					List.of("Слова, которые обычно употребляются вместе",
							"Слова, которые редко используются",
							"Слова, заученные по списку",
							"Слова без перевода на русский"),
					"Слова, которые обычно употребляются вместе",
					"Пример «make a decision» вместо «do a decision» показывает, что в английском есть устойчивые сочетания слов."),
			new GeneratedQuestion(
					"Какой способ изучения слов автор считает менее надёжным?",
					List.of("Чтение текстов",
							"Запоминание списков слов",
							"Чтение с догадкой по контексту",
							"Пересказ прочитанного своими словами"),
					"Запоминание списков слов",
					"Слова из списка не имеют контекста и забываются быстрее, чем слова из живого текста."),
			new GeneratedQuestion(
					"Почему автор предлагает угадывать значение слова по контексту?",
					List.of("Чтобы не открывать словарь вообще",
							"Чтобы читать быстрее",
							"Чтобы научиться понимать текст напрямую, без постоянного перевода",
							"Чтобы запомнить перевод"),
					"Чтобы научиться понимать текст напрямую, без постоянного перевода",
					"Такая тренировка развивает навык чтения, при котором смысл воспринимается сразу, без дословного перевода."),
			new GeneratedQuestion(
					"Что автор считает важнее: объём чтения или его регулярность?",
					List.of("Регулярность",
							"Объём за один подход",
							"Скорость чтения",
							"Выбор сложных текстов"),
					"Регулярность",
					"Прямо сказано: постоянство важнее интенсивности, 15 минут в день лучше трёх часов раз в месяц."));

	@Override
	public List<TopicProposal> generateTopics(Profile profile) {
		return TOPICS;
	}

	@Override
	public GeneratedText generateText(TopicProposal topic, Profile profile) {
		return new GeneratedText(topic.title(), topic.description(), TEXT);
	}

	@Override
	public List<GeneratedQuestion> generateQuestions(GeneratedText text, Profile profile) {
		return QUESTIONS;
	}

	@Override
	public String continueDiscussion(LearningSession session, String userMessage) {
		return switch ((int) session.userTurns()) {
			case 0 -> "Which part of the text did you find most useful?";
			case 1 -> "Do you read in English regularly? What makes it easy or hard for you?";
			case 2 -> "The author says consistency beats intensity. Do you agree?";
			case 3 -> "How would you explain the main idea of the text to a friend?";
			default -> "What habit from the text would you like to try this week?";
		};
	}

	@Override
	public ReviewData generateReview(LearningSession session) {
		return new ReviewData(
				"Вы прочитали текст о том, почему ежедневное чтение эффективнее разовых занятий.",
				"Разговор держался темы текста, вы отвечали по содержанию.",
				"Стоит шире практиковать устойчивые сочетания слов и реже переводить прочитанное дословно.",
				session.getLevel(),
				List.of("Читать по 15 минут каждый день",
						"Отмечать в тексте интересные сочетания слов",
						"Сначала догадываться о значении, потом проверять"));
	}

}