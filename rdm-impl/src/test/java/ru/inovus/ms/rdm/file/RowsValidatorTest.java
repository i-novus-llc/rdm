package ru.inovus.ms.rdm.file;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Collections;
import java.util.HashMap;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.inovus.ms.rdm.file.BufferedRowsPersisterTest.createTestStructure;

@RunWith(MockitoJUnitRunner.class)
public class RowsValidatorTest {

    private RowsValidator rowsValidator;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private VersionService versionService;

    private static final String ATTRIBUTE_NAME = "ref_name";

    private static final String ATTRIBUTE_VALUE = "ref_value";

    private static final String REFERENCE_ATTRIBUTE = "name";

    private static final Integer REFERENCE_VERSION = 1;

    private SearchDataCriteria searchDataCriteria;

    @Before
    public void setUp() {
        rowsValidator = new RowsValidatorImpl(versionService, fieldFactory, createTestStructureWithReference());
        when(fieldFactory.createField(eq(REFERENCE_ATTRIBUTE), eq(FieldType.STRING))).thenReturn(new StringField(REFERENCE_ATTRIBUTE));
        when(versionService.getStructure(eq(REFERENCE_VERSION))).thenReturn(createTestStructure());
        StringField fieldFilter = new StringField(REFERENCE_ATTRIBUTE);
        FieldSearchCriteria searchCriteria = new FieldSearchCriteria(fieldFilter, SearchTypeEnum.EXACT, Collections.singletonList(ATTRIBUTE_VALUE));
        searchDataCriteria = new SearchDataCriteria(Collections.singletonList(searchCriteria), null);
        when(versionService.search(eq(REFERENCE_VERSION), eq(searchDataCriteria)))
                .thenReturn(new PageImpl<>(Collections.singletonList(new LongRowValue())));
    }

    @Test
    public void testAppendAndProcess() {
        Row row = createTestRowWithReference();
        Result expected = new Result(1, 1, null);

        Result appendActual = rowsValidator.append(row);
        Result processActual = rowsValidator.process();

        Assert.assertEquals(expected, appendActual);
        Assert.assertEquals(expected, processActual);
    }

    @Test
    public void testAppendAndProcessWithErrors() {
        Row validRow = createTestRowWithReference();
        Row notValidRow = new Row(new HashMap() {{
            put("count", 1);
        }});
        Result expected = new Result(1, 2, Collections.singletonList("Reference in row is not valid"));
        when(versionService.search(AdditionalMatchers.not(eq(REFERENCE_VERSION)), AdditionalMatchers.not(eq(searchDataCriteria))))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        rowsValidator.append(validRow);
        Result appendActual = rowsValidator.append(notValidRow);
        Result processActual = rowsValidator.process();

        Assert.assertEquals(expected, appendActual);
        Assert.assertEquals(expected, processActual);
    }

    private Row createTestRowWithReference() {
        return new Row(new HashMap() {{
            put(ATTRIBUTE_NAME, ATTRIBUTE_VALUE);
        }});
    }

    private Structure createTestStructureWithReference() {
        Structure structure = new Structure();
        structure.setAttributes(Collections.singletonList(Structure.Attribute.build(ATTRIBUTE_NAME , ATTRIBUTE_NAME, FieldType.REFERENCE, false, "description")));
        structure.setReferences(Collections.singletonList(new Structure.Reference(ATTRIBUTE_NAME, REFERENCE_VERSION, REFERENCE_ATTRIBUTE, null)));
        return structure;
    }
}
