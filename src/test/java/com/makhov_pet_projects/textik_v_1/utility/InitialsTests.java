package com.makhov_pet_projects.textik_v_1.utility;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Инициалы из имени")
class InitialsTests {

	@Test
	@DisplayName("Тест, в котором мы проверяем, что из одного слова получается первая заглавная буква")
	void singleWordGivesFirstLetter() {
		//Act
		String initials = Initials.of("Sergey");

		//Assert
		assertThat(initials).isEqualTo("S");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что из двух слов получаются две первые буквы")
	void twoWordsGiveTwoLetters() {
		//Act
		String initials = Initials.of("иван петров");

		//Assert
		assertThat(initials).isEqualTo("ИП");
	}

	@Test
	@DisplayName("Тест, в котором мы проверяем, что пустое имя даёт заполнитель")
	void blankNameGivesPlaceholder() {
		//Act
		String initials = Initials.of(null);

		//Assert
		assertThat(initials).isEqualTo("?");
	}

}