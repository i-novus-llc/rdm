package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

class DefaultReferrerToDefaultTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным справочником, ссылающимся на обычный справочник.
     */
    @Test
    void testDefaultReferrerToDefault() {
        testReferrerRefBook(null, null);
    }

}
