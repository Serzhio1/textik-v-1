package com.makhov_pet_projects.textik_v_1.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.makhov_pet_projects.textik_v_1.dto.DiscussionTurn;
import com.makhov_pet_projects.textik_v_1.dto.TopicProposal;
import com.makhov_pet_projects.textik_v_1.entity.LearningSession;
import com.makhov_pet_projects.textik_v_1.entity.Profile;
import com.makhov_pet_projects.textik_v_1.exceptions.AiGatewayException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Переключение на резервную модель")
class FallbackAiGatewayTests {

	private final Profile profile = Profile.of(1L, "Люблю IT", "технологии", "A2");

	private final List<TopicProposal> topics =
			List.of(new TopicProposal("Резервная тема", "Описание"));

	@Test
	@DisplayName("Тест, в котором мы проверяем, что при успешной основной модели резервная не вызывается")
	void usesPrimaryWhenItSucceeds() {
		//Arrange
		AiGateway primary = mock(AiGateway.class);
		AiGateway backup = mock(AiGateway.class);
		when(primary.generateTopics(profile)).thenReturn(List.of(new TopicProposal("Основная тема", "Описание")));

		//Act
		List<TopicProposal> result = new FallbackAiGateway(primary, backup).generateTopics(profile);

		//Assert
		assertThat(result.getFirst().title()).isEqualTo("Основная тема");
		verifyNoInteractions(backup);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что при сбое основной модели ответ берётся у резервной")
	void usesFallbackWhenPrimaryFails() {
		//Arrange
		AiGateway primary = mock(AiGateway.class);
		AiGateway backup = mock(AiGateway.class);
		when(primary.generateTopics(profile)).thenThrow(new AiGatewayException("endpoint недоступен"));
		when(backup.generateTopics(profile)).thenReturn(topics);

		//Act
		List<TopicProposal> result = new FallbackAiGateway(primary, backup).generateTopics(profile);

		//Assert
		assertThat(result).isEqualTo(topics);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что начало обсуждения переключается на резервную модель")
	void opensDiscussionWithFallback() {
		//Arrange
		AiGateway primary = mock(AiGateway.class);
		AiGateway backup = mock(AiGateway.class);
		LearningSession session = mock(LearningSession.class);
		when(primary.openDiscussion(session)).thenThrow(new AiGatewayException("основная упала"));
		when(backup.openDiscussion(session)).thenReturn("Which part did you like most?");

		//Act
		String opening = new FallbackAiGateway(primary, backup).openDiscussion(session);

		//Assert
		assertThat(opening).isEqualTo("Which part did you like most?");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что продолжение обсуждения переключается на резервную модель")
	void continuesDiscussionWithFallback() {
		//Arrange
		AiGateway primary = mock(AiGateway.class);
		AiGateway backup = mock(AiGateway.class);
		LearningSession session = mock(LearningSession.class);
		DiscussionTurn turn = new DiscussionTurn("Good point.", "I have a question.");
		when(primary.continueDiscussion(session, "I has a question."))
				.thenThrow(new AiGatewayException("основная упала"));
		when(backup.continueDiscussion(session, "I has a question.")).thenReturn(turn);

		//Act
		DiscussionTurn result = new FallbackAiGateway(primary, backup)
				.continueDiscussion(session, "I has a question.");

		//Assert
		assertThat(result).isEqualTo(turn);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что при сбое обеих моделей выбрасывается AiGatewayException")
	void failsWhenBothModelsFail() {
		//Arrange
		AiGateway primary = mock(AiGateway.class);
		AiGateway backup = mock(AiGateway.class);
		when(primary.generateTopics(profile)).thenThrow(new AiGatewayException("основная упала"));
		when(backup.generateTopics(profile)).thenThrow(new AiGatewayException("резервная упала"));

		//Assert
		assertThatThrownBy(() -> new FallbackAiGateway(primary, backup).generateTopics(profile))
				.isInstanceOf(AiGatewayException.class)
				.hasMessage("не удалось подобрать темы")
				.hasSuppressedException(new AiGatewayException("резервная упала"));
	}

}