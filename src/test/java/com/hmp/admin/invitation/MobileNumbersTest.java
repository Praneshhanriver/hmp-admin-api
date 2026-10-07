package com.hmp.admin.invitation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MobileNumbersTest {

	@ParameterizedTest
	@ValueSource(strings = { "010-1234-5678", "01012345678", "010-12345678", "011-123-4567", "0161234567" })
	void acceptsKoreanMobileWithOrWithoutHyphens(String mobile) {
		assertThat(mobile).matches(MobileNumbers.PATTERN);
	}

	@ParameterizedTest
	@ValueSource(strings = { "123", "020-1234-5678", "010-1234-567", "010 1234 5678", "010-1234-56789", "abc-defg-hijk" })
	void rejectsAnythingElse(String mobile) {
		assertThat(mobile).doesNotMatch(MobileNumbers.PATTERN);
	}

	@Test
	void normalizesToHyphenatedForm() {
		assertThat(MobileNumbers.normalize("01012345678")).isEqualTo("010-1234-5678");
		assertThat(MobileNumbers.normalize("0111234567")).isEqualTo("011-123-4567");
	}

	@Test
	void masksTheMiddleDigits() {
		assertThat(MobileNumbers.mask("010-1234-5678")).isEqualTo("010-****-5678");
		assertThat(MobileNumbers.mask(null)).isNull();
	}

	@Test
	void visibleDigitsAreFirstThreeAndLastFour() {
		assertThat(MobileNumbers.visibleDigits("010-1234-5678")).isEqualTo("0105678");
	}

}
