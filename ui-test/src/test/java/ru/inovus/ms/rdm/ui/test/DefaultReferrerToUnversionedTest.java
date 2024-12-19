package ru.inovus.ms.rdm.ui.test;

import org.junit.jupiter.api.Test;
import ru.inovus.ms.rdm.ui.test.model.RefBook;

class DefaultReferrerToUnversionedTest extends AbstractRdmUiTest {

    /**
     * Проверка работы с обычным справочником, ссылающимся на неверсионный справочник.
     */
    @Test
    void testDefaultReferrerToUnversioned() {
        testReferrerRefBook(RefBook.getUnversionedType(), null);
    }

}
