package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

import static ru.inovus.ms.rdm.ui.test.model.RefBook.getUnversionedType;

public class UnversionedRefBookTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с неверсионным справочником.
     */
    @Test
    void testRefBook() {
        runUiTest(refBookListPage -> testRefBook(refBookListPage, getUnversionedType()));
    }
}
