package ru.i_novus.ms.rdm.api.model.version;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;

public class RefBookVersionTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";
    private static final Integer VERSION_ID = 2;

    @Test
    public void testEmpty() {

        RefBookVersion empty = new RefBookVersion();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        RefBookVersion empty = new RefBookVersion();
        RefBookVersion model = createModel();

        assertObjects(Assert::assertNotEquals, empty, model);
    }

    @Test
    public void testCopy() {

        RefBookVersion model = createModel();
        RefBookVersion copyModel = new RefBookVersion(model);

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    private RefBookVersion createModel() {

        RefBookVersion model = new RefBookVersion();
        model.setId(VERSION_ID);
        model.setRefBookId(REFBOOK_ID);
        model.setCode(REFBOOK_CODE);

        return model;
    }
}