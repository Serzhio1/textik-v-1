package com.makhov_pet_projects.textik_v_1.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.makhov_pet_projects.textik_v_1.service.AiGateway;
import com.makhov_pet_projects.textik_v_1.service.FallbackAiGateway;
import com.makhov_pet_projects.textik_v_1.service.SpringAiGateway;
import com.makhov_pet_projects.textik_v_1.service.StubAiGateway;
import com.makhov_pet_projects.textik_v_1.support.FakeChatModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("Выбор реализации слоя ИИ по конфигурации")
class AiGatewaySelectionTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(AiGatewayConfig.class)
			.withBean(ChatClient.Builder.class, () -> ChatClient.builder(FakeChatModel.answeringWith("{}")));

	@Test
	@DisplayName("OpenAI-совместимый провайдер подключает реализацию на Spring AI")
	void usesSpringAiForOpenAiProvider() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=openai").run(context -> {
			//Assert
			assertThat(context).hasSingleBean(AiGateway.class);
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(SpringAiGateway.class);
		});
	}

	@Test
	@DisplayName("GigaChat тоже работает через реализацию на Spring AI: меняется только клиент")
	void usesSpringAiForGigaChatProvider() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=gigachat").run(context -> {
			//Assert
			assertThat(context).hasSingleBean(AiGateway.class);
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(SpringAiGateway.class);
		});
	}

	@Test
	@DisplayName("Провайдер stub отдаёт заглушку без обращения к сети")
	void usesStubForStubProvider() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=stub").run(context -> {
			//Assert
			assertThat(context).hasSingleBean(AiGateway.class);
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(StubAiGateway.class);
		});
	}

	@Test
	@DisplayName("Профиль local по-прежнему отдаёт заглушку")
	void usesStubInLocalProfile() {
		//Arrange
		//Act
		runner.withPropertyValues("spring.profiles.active=local").run(context -> {
			//Assert
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(StubAiGateway.class);
		});
	}

	@Test
	@DisplayName("Резервная заглушка оборачивает удалённого провайдера")
	void wrapsRemoteGatewayWithFallback() {
		//Arrange
		//Act
		runner.withPropertyValues("app.ai.provider=openai", "app.ai.fallback=stub").run(context -> {
			//Assert
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(FallbackAiGateway.class);
		});
	}

	@Test
	@DisplayName("Одновременно выбранные профиль и провайдер не заводят второй бин")
	void keepsSingleGatewayForLocalProfileAndStubProvider() {
		//Arrange
		//Act
		runner.withPropertyValues("spring.profiles.active=local", "app.ai.provider=stub").run(context -> {
			//Assert
			assertThat(context).hasSingleBean(AiGateway.class);
			assertThat(context.getBean(AiGateway.class)).isInstanceOf(StubAiGateway.class);
		});
	}

}