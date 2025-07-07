package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

import static ru.inovus.ms.rdm.ui.test.model.RefBook.getUnversionedType;

class DefaultReferrerToUnversionedTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным справочником, ссылающимся на неверсионный справочник.
     */
    @Test
    void testReferrer() {
        runUiTest(refBookListPage -> testReferrerRefBook(refBookListPage, getUnversionedType(), null));
    }
}
