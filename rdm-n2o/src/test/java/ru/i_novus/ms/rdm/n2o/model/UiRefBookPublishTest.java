package ru.i_novus.ms.rdm.n2o.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.util.json.JsonUtil;
import ru.i_novus.ms.rdm.n2o.BaseTest;

import static org.junit.Assert.assertEquals;

public class UiRefBookPublishTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";
    private static final Integer DRAFT_ID = 2;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    @SuppressWarnings("java:S2696")
    public void setUp() {
        JsonUtil.jsonMapper = objectMapper;
    }

    @Test
    public void testEmpty() {

        UiRefBook empty = createEmpty();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        UiRefBook empty = createEmpty();
        UiRefBook model = createModel();

        assertObjects(Assert::assertNotEquals, empty, model);
        assertEquals(REFBOOK_ID, model.getRefBookId());
        assertEquals(REFBOOK_CODE, model.getCode());
        assertEquals(DRAFT_ID, model.getDraftVersionId());
    }

    @Test
    public void testCopy() {

        UiRefBookPublish model = createModel();
        UiRefBookPublish copyModel = new UiRefBookPublish(model);

        assertObjects(Assert::assertEquals, model, copyModel);
    }

    private UiRefBookPublish createEmpty() {

        UiRefBook uiRefBook = new UiRefBook(new RefBook());
        return new UiRefBookPublish(uiRefBook);
    }

    private UiRefBookPublish createModel() {

        UiRefBook uiRefBook = new UiRefBook(createRefBook());
        return new UiRefBookPublish(uiRefBook);
    }

    private RefBook createRefBook() {

        RefBook result = new RefBook();
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);
        result.setDraftVersionId(DRAFT_ID);

        return result;
    }
}