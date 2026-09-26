package com.makhov_pet_projects.textik_v_1.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Статус учебной сессии")
class SessionStatusTests {

	@Test
	@DisplayName("Тест, в котором мы проверяем, что предложенная сессия переходит в чтение")
	void proposedGoesToRead() {
		//Arrange
		SessionStatus status = SessionStatus.PROPOSED;

		//Act
		SessionStatus next = status.next();

		//Assert
		assertThat(next).isEqualTo(SessionStatus.READ);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что чтение переходит к вопросам")
	void readGoesToThink() {
		//Arrange
		SessionStatus status = SessionStatus.READ;

		//Act
		SessionStatus next = status.next();

		//Assert
		assertThat(next).isEqualTo(SessionStatus.THINK);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что вопросы переходят к обсуждению")
	void thinkGoesToDiscuss() {
		//Arrange
		SessionStatus status = SessionStatus.THINK;

		//Act
		SessionStatus next = status.next();

		//Assert
		assertThat(next).isEqualTo(SessionStatus.DISCUSS);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что обсуждение переходит к завершённой сессии")
	void discussGoesToDone() {
		//Arrange
		SessionStatus status = SessionStatus.DISCUSS;

		//Act
		SessionStatus next = status.next();

		//Assert
		assertThat(next).isEqualTo(SessionStatus.DONE);
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что завершённая сессия не двигается дальше")
	void doneStaysDone() {
		//Arrange
		SessionStatus status = SessionStatus.DONE;

		//Act
		SessionStatus next = status.next();

		//Assert
		assertThat(next).isEqualTo(SessionStatus.DONE);
	}

}
