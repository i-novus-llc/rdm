package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.platform.i18n.Message;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.StringField;
import ru.inovus.ms.rdm.model.Structure;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static ru.inovus.ms.rdm.validation.resolver.UniqueAttributeValidationResolver.DB_CONTAINS_VALUE_EXCEPTION_CODE;
import static ru.inovus.ms.rdm.validation.resolver.UniqueAttributeValidationResolver.VALUE_NOT_UNIQUE_EXCEPTION_CODE;

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
        when(searchDataService.getPagedData(Matchers.any())).thenReturn(new CollectionPage<>());
        when(searchDataService.getPagedData(argThat(new ArgumentMatcher<DataCriteria>() {
            @Override
            public boolean matches(Object o) {
                return DB_CONTAINS_STRING.equals(
                        ((DataCriteria) o).getFieldFilter().iterator().next().get(0).getValues().get(0));
            }
        }))).thenReturn(new CollectionPage<>(
                1,
                singletonList(new LongRowValue(new StringField(TEST_ATTRIBUTE).valueOf(DB_CONTAINS_STRING))),
                new Criteria()));
    }

    @Test
    public void testResolve() {

        UniqueAttributeValidationResolver resolver = new UniqueAttributeValidationResolver(attribute, searchDataService, TEST_STORAGE_CODE);
        assertNull(resolver.resolve(UNIQUE_STRING));
        Message actual = resolver.resolve(UNIQUE_STRING);
        assertEquals(VALUE_NOT_UNIQUE_EXCEPTION_CODE, actual.getCode());
        actual = resolver.resolve(DB_CONTAINS_STRING);
        assertEquals(DB_CONTAINS_VALUE_EXCEPTION_CODE, actual.getCode());

    }


}