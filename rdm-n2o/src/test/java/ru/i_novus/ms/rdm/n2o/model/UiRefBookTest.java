package ru.i_novus.ms.rdm.n2o.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.BaseTest;

import static org.junit.Assert.assertEquals;

public class UiRefBookTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testEmpty() {

        UiRefBook empty = new UiRefBook(new RefBook());
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        UiRefBook empty = new UiRefBook(new RefBook());
        UiRefBook model = createModel();

        assertObjects(Assert::assertNotEquals, empty, model);
        assertEquals(REFBOOK_ID, model.getRefBookId());
        assertEquals(REFBOOK_CODE, model.getCode());
    }

    @Test
    public void testCopy() {

        UiRefBook model = createModel();

        UiRefBook copyModel = new UiRefBook(new RefBook());
        copyModel.setRefBookId(model.getRefBookId());
        copyModel.setCode(model.getCode());

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    private UiRefBook createModel() {

        return new UiRefBook(createRefBook());
    }

    private RefBook createRefBook() {

        RefBook result = new RefBook();
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);

        return result;
    }
}