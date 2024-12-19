package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

public class DefaultReferrerToDefaultTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным справочником, ссылающимся на обычный справочник.
     */
    @Test
    void testReferrer() {
        runUiTest(refBookListPage -> testReferrerRefBook(refBookListPage, null, null));
    }
}
