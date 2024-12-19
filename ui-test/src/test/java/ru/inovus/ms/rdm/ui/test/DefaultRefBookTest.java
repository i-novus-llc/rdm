package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

public class DefaultRefBookTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным (версионным) справочником.
     */
    @Test
    void testRefBook() {
        runUiTest(refBookListPage -> testRefBook(refBookListPage, null));
    }
}
