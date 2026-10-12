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
			new TopicProposal("Reading for Progress", "Why daily reading beats one-off study sessions"),
			new TopicProposal("Notetaking That Works", "How to take notes you will actually return to"),
			new TopicProposal("Small Habits, Big Results", "Why small daily habits beat rare heroic efforts"));

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
					"Why does the author advise choosing a text that matches your level?",
					List.of("To read faster",
							"So that the text is too easy",
							"So that you keep reading and still meet new words",
							"So that you spend less time"),
					"So that you keep reading and still meet new words",
					"A text that is too difficult makes you stop, and one that is too easy gives almost no progress. You need a balance."),
			new GeneratedQuestion(
					"What are collocations according to the text?",
					List.of("Words that usually go together",
							"Words that are rarely used",
							"Words learnt from a list",
							"Words without a translation into Russian"),
					"Words that usually go together",
					"The example \"make a decision\" instead of \"do a decision\" shows that English has fixed word combinations."),
			new GeneratedQuestion(
					"Which way of learning words does the author consider less reliable?",
					List.of("Reading texts",
							"Memorising word lists",
							"Guessing words from context",
							"Retelling what you read in your own words"),
					"Memorising word lists",
					"Words from a list have no context and are forgotten faster than words met in a real text."),
			new GeneratedQuestion(
					"Why does the author suggest guessing the meaning of a word from the context?",
					List.of("So that you never open a dictionary",
							"To read faster",
							"To learn to understand the text directly, without constant translation",
							"To remember the translation"),
					"To learn to understand the text directly, without constant translation",
					"This exercise trains reading in which the meaning is understood at once, without word-for-word translation."),
			new GeneratedQuestion(
					"What does the author think is more important: the amount of reading or how regular it is?",
					List.of("Regularity",
							"The amount in one sitting",
							"Reading speed",
							"Choosing difficult texts"),
					"Regularity",
					"The text says clearly: consistency beats intensity; fifteen minutes a day beats three hours once a month."));

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
				"You read a text about why daily reading is more effective than one-off study sessions.",
				"The conversation stayed on the topic of the text and you answered in full sentences.",
				"It is worth practising common word combinations more and translating what you read less.",
				session.getLevel(),
				List.of("Read for fifteen minutes every day",
						"Note interesting word combinations in the text",
						"Guess the meaning first and only then check it"));
	}

}