package ru.i_novus.ms.rdm.api.model.version;

import org.junit.Assert;
import org.junit.Test;
import ru.i_novus.ms.rdm.api.BaseTest;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;

import static org.junit.Assert.assertEquals;

public class AttributeFilterTest extends BaseTest {

    @Test
    public void testEmpty() {

        AttributeFilter empty = new AttributeFilter();
        assertSpecialEquals(empty);
    }

    @Test
    public void testClass() {

        AttributeFilter empty = new AttributeFilter();
        AttributeFilter model = new AttributeFilter("id", 1, FieldType.INTEGER);

        assertObjects(Assert::assertNotEquals, empty, model);

        AttributeFilter fullModel = new AttributeFilter("id", 1, FieldType.INTEGER, null);
        assertEquals(model, fullModel);
    }

    @Test
    public void testGetSearchTypeWhenNull() {

        AttributeFilter model = new AttributeFilter("id", 1, FieldType.INTEGER);
        assertEquals(SearchTypeEnum.EXACT, model.getSearchType());

        model = new AttributeFilter("name", "caption", FieldType.STRING);
        assertEquals(SearchTypeEnum.LIKE, model.getSearchType());
    }

    @Test
    public void testGetSearchTypeWhenNotNull() {

        AttributeFilter model = new AttributeFilter("id", 1, FieldType.INTEGER, SearchTypeEnum.EXACT);
        assertEquals(SearchTypeEnum.EXACT, model.getSearchType());
    }
}