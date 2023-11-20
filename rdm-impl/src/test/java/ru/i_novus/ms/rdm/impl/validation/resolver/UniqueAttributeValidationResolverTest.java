package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UniqueAttributeValue;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataPage;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.validation.resolver.UniqueAttributeValidationResolver.DB_CONTAINS_VALUE_EXCEPTION_CODE;
import static ru.i_novus.ms.rdm.impl.validation.resolver.UniqueAttributeValidationResolver.VALUE_NOT_UNIQUE_EXCEPTION_CODE;

@RunWith(MockitoJUnitRunner.class)
public class UniqueAttributeValidationResolverTest {
    private final String TEST_ATTRIBUTE = "test_attribute";
    private final String TEST_STORAGE_CODE = "test_storage_code";
    private final String UNIQUE_STRING = "unique";
    private final String DB_CONTAINS_STRING = "db";
    private final Structure.Attribute attribute =
            Structure.Attribute.build(TEST_ATTRIBUTE, TEST_ATTRIBUTE, FieldType.STRING, null);

    @Mock
    SearchDataService searchDataService;

    @Before
    public void init() {
        when(searchDataService.getPagedData(any()))
                .thenReturn(new DataPage<>(0, emptyList(), new DataCriteria()));
        when(searchDataService.getPagedData(
                argThat(o -> DB_CONTAINS_STRING.equals(
                        o.getFieldFilters().iterator().next().get(0).getValues().get(0))
                )
        )).thenReturn(new DataPage<>(
                1,
                singletonList(new LongRowValue(1L, singletonList(new StringField(TEST_ATTRIBUTE).valueOf(DB_CONTAINS_STRING)))),
                new DataCriteria()));
    }

    @Test
    public void testResolve() {
        UniqueAttributeValidationResolver resolver = new UniqueAttributeValidationResolver(attribute, searchDataService, TEST_STORAGE_CODE);
        assertNull(resolver.resolve(new UniqueAttributeValue(null, UNIQUE_STRING)));
        Message actual = resolver.resolve(new UniqueAttributeValue(null, UNIQUE_STRING));
        assertEquals(VALUE_NOT_UNIQUE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(new UniqueAttributeValue(null, DB_CONTAINS_STRING));
        assertEquals(DB_CONTAINS_VALUE_EXCEPTION_CODE, actual.getCode());
    }
}