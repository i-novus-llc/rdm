package ru.i_novus.ms.rdm.api.model.refbook;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

import static org.junit.Assert.assertEquals;

public class RefBookTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";

    @Test
    public void testEmpty() {

        RefBook empty = new RefBook();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        RefBook empty = new RefBook();

        RefBook model = createModel();
        assertObjects(Assert::assertNotEquals, empty, model);
    }

    @Test
    public void testCopy() {

        RefBook model = createModel();
        RefBook copyModel = new RefBook(model);

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    @Test
    public void testVersionCopy() {

        RefBookVersion model = new RefBookVersion();
        model.setRefBookId(REFBOOK_ID);
        model.setCode(REFBOOK_CODE);

        RefBook copyVersion = new RefBook(model);
        assertObjects(Assert::assertNotEquals, model, copyVersion);

        assertEquals(model.getRefBookId(), copyVersion.getRefBookId());
        assertEquals(model.getCode(), copyVersion.getCode());
    }

    private RefBook createModel() {

        RefBook model = new RefBook();
        model.setRefBookId(REFBOOK_ID);
        model.setCode(REFBOOK_CODE);
        model.setCurrentOperation(RefBookOperation.PUBLISHING);

        return model;
    }
}