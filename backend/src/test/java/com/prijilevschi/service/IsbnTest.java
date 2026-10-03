package com.prijilevschi.service;

import com.prijilevschi.error.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsbnTest {

    @Test
    void normalizesHyphensSpacesAndLowerCaseX() {
        assertThat(Isbn.normalize(" 978-0-306-40615-7 ")).isEqualTo("9780306406157");
        assertThat(Isbn.normalize("0-8044-2957-x")).isEqualTo("080442957X");
        assertThat(Isbn.normalize("  ")).isNull();
        assertThat(Isbn.normalize(null)).isNull();
    }

    @Test
    void validatesChecksums() {
        assertThat(Isbn.isValid("9780306406157")).isTrue();
        assertThat(Isbn.isValid("0306406152")).isTrue();
        assertThat(Isbn.isValid("080442957X")).isTrue();
        assertThat(Isbn.isValid("9780306406158")).isFalse();
        assertThat(Isbn.isValid("0306406153")).isFalse();
        assertThat(Isbn.isValid("12345")).isFalse();
    }

    @Test
    void rejectsInvalidButAllowsBlank() {
        assertThat(Isbn.requireValidOrNull("")).isNull();
        assertThatThrownBy(() -> Isbn.requireValidOrNull("978-0-306-40615-8"))
                .isInstanceOf(BadRequestException.class);
    }
}
