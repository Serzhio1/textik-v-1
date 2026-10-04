package com.makhov_pet_projects.textik_v_1.service;

import com.makhov_pet_projects.textik_v_1.dto.GeneratedQuestion;
import com.makhov_pet_projects.textik_v_1.dto.GeneratedText;
import com.makhov_pet_projects.textik_v_1.dto.ReviewData;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import java.util.List;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class FallbackAiGateway implements AiGateway {

	private final AiGateway primary;
	private final AiGateway fallback;

	@Override
	public List<TopicProposal> generateTopics(Profile profile) {
		return withFallback(
				() -> primary.generateTopics(profile),
				() -> fallback.generateTopics(profile),
				"не удалось подобрать темы");
	}

	@Override
	public GeneratedText generateText(TopicProposal topic, Profile profile) {
		return withFallback(
				() -> primary.generateText(topic, profile),
				() -> fallback.generateText(topic, profile),
				"не удалось подготовить текст");
	}

	@Override
	public List<GeneratedQuestion> generateQuestions(GeneratedText text, Profile profile) {
		return withFallback(
				() -> primary.generateQuestions(text, profile),
				() -> fallback.generateQuestions(text, profile),
				"не удалось составить вопросы");
	}

	@Override
	public String continueDiscussion(LearningSession session, String userMessage) {
		return withFallback(
				() -> primary.continueDiscussion(session, userMessage),
				() -> fallback.continueDiscussion(session, userMessage),
				"не удалось получить ответ собеседника");
	}

	@Override
	public ReviewData generateReview(LearningSession session) {
		return withFallback(
				() -> primary.generateReview(session),
				() -> fallback.generateReview(session),
				"не удалось подвести итоги");
	}

	private <T> T withFallback(Supplier<T> primaryCall, Supplier<T> fallbackCall, String message) {
		try {
			return primaryCall.get();
		} catch (AiGatewayException primaryFailure) {
			log.warn("Основная модель не сработала ({}), переключаемся на резервную", primaryFailure.getMessage());
			try {
				return fallbackCall.get();
			} catch (AiGatewayException fallbackFailure) {
				AiGatewayException bothFailed = new AiGatewayException(message, primaryFailure);
				bothFailed.addSuppressed(fallbackFailure);
				throw bothFailed;
			}
		}
	}

}