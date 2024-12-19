package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;

class DefaultRefBookTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным (версионным) справочником.
     */
    @Test
    void testCreateDefaultRefBook() {
        testRefBook(null);
    }

}
