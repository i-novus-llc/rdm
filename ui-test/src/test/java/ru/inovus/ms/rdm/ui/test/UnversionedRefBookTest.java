package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;
import ru.inovus.ms.rdm.ui.test.model.RefBook;

class UnversionedRefBookTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с неверсионным справочником.
     */
    @Test
    void testUnversionedRefBook() {
        testRefBook(RefBook.getUnversionedType());
    }

}
