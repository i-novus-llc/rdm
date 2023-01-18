package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.dao.query.AbstractField;
import net.n2oapp.framework.api.metadata.global.dao.query.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.query.field.QuerySimpleField;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordQueryResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.api.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.REFERENCE_DISPLAY_VALUE;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.REFERENCE_VALUE;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addFieldProperty;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@RunWith(MockitoJUnitRunner.class)
public class DataRecordQueryProviderTest extends BaseRecordProviderTest {

    @InjectMocks
    private DataRecordQueryProvider provider;

    @Mock
    protected VersionRestService versionService;

    @Spy
    private final Collection<DataRecordQueryResolver> resolvers = new ArrayList<>(1);

    @Before
    public void setUp() {

        DataRecordQueryResolver resolver = new TestRecordQueryResolver();
        resolvers.add(resolver);
    }

    @Test
    public void testGetCode() {

        String providerCode = provider.getCode();
        assertFalse(isEmpty(providerCode));
    }

    @Test
    public void testRead() {
        super.testRead();
    }

    @Override
    void testRead(Structure structure) {

        if (structure != null) {
            when(versionService.getStructure(eq(TEST_REFBOOK_DRAFT_ID))).thenReturn(structure);

        } else {
            structure = Structure.EMPTY;
            when(versionService.getStructure(eq(TEST_REFBOOK_DRAFT_ID))).thenThrow(new IllegalArgumentException());
        }

        List<? extends SourceMetadata> list = provider.read(TEST_CONTEXT);
        assertNotNull(list);
        assertEquals(1, list.size());

        SourceMetadata metadata = list.get(0);
        assertTrue(metadata instanceof N2oQuery);

        N2oQuery query = (N2oQuery) metadata;
        assertNotNull(query.getUniques());
        assertEquals(1, query.getUniques().length);

        List<AbstractField> items = Arrays.asList(query.getFields());
        assertNotNull(items);
        assertTrue(items.size() >= structure.getAttributes().size());

        structure.getAttributes().forEach(attribute -> {

            final String codeWithPrefix = addPrefix(attribute.getCode());
            if (attribute.isReferenceType()) {

                assertTrue(existsItem(items, addFieldProperty(codeWithPrefix, REFERENCE_VALUE)));
                assertTrue(existsItem(items, addFieldProperty(codeWithPrefix, REFERENCE_DISPLAY_VALUE)));

            } else {
                assertTrue(existsItem(items, codeWithPrefix));
            }
        });
    }

    private boolean existsItem(List<AbstractField> items, String id) {

        return items.stream().anyMatch(item -> id.equals(getFieldId(item)));
    }

    private String getFieldId(AbstractField item) {

        return item != null ? item.getId() : null;
    }

    @Test
    public void testGetMetadataClasses() {

        Collection<Class<? extends SourceMetadata>> list = provider.getMetadataClasses();
        assertNotNull(list);
    }

    private static class TestRecordQueryResolver implements DataRecordQueryResolver {

        @Override
        public boolean isSatisfied(String dataAction) {
            return TEST_ACTION.equals(dataAction);
        }

        @Override
        public List<QuerySimpleField> createRegularFields(DataRecordRequest request) {
            return emptyList();
        }

        @Override
        public List<N2oQuery.Filter> createRegularFilters(DataRecordRequest request) {
            return emptyList();
        }
    }
}