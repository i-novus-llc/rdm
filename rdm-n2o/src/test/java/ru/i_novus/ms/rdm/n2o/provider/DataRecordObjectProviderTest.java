package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
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
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordObjectResolver;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.REFERENCE_VALUE;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addFieldProperty;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@RunWith(MockitoJUnitRunner.class)
public class DataRecordObjectProviderTest extends BaseRecordProviderTest {

    @Spy
    private final Collection<DataRecordObjectResolver> resolvers = new ArrayList<>(1);

    @Mock
    protected VersionRestService versionService;
    @InjectMocks
    private DataRecordObjectProvider provider;

    @Before
    public void setUp() {

        DataRecordObjectResolver resolver = new TestRecordObjectResolver();
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
        assertTrue(metadata instanceof N2oObject);

        N2oObject object = (N2oObject) metadata;
        assertNotNull(object.getOperations());
        assertEquals(1, object.getOperations().length);

        N2oObject.Operation operation = object.getOperations()[0];

        List<AbstractParameter> items = Arrays.asList(operation.getInFields());
        assertNotNull(items);
        assertTrue(items.size() >= structure.getAttributes().size());

        structure.getAttributes().forEach(attribute -> {

            final String codeWithPrefix = addPrefix(attribute.getCode());
            if (attribute.isReferenceType()) {

                assertTrue(existsItem(items, addFieldProperty(codeWithPrefix, REFERENCE_VALUE)));

            } else {
                assertTrue(existsItem(items, codeWithPrefix));
            }
        });
    }

    private boolean existsItem(List<AbstractParameter> items, String id) {

        return items.stream().anyMatch(item -> id.equals(getFieldId(item)));
    }

    private String getFieldId(AbstractParameter item) {

        return item.getId();
    }

    @Test
    public void getMetadataClasses() {

        Collection<Class<? extends SourceMetadata>> list = provider.getMetadataClasses();
        assertNotNull(list);
    }

    private static class TestRecordObjectResolver implements DataRecordObjectResolver {

        private static final String TEST_OPERATION = "trialize";
        private static final int ROW_ARGUMENT_INDEX = 0;

        @Override
        public boolean isSatisfied(String dataAction) {
            return TEST_ACTION.equals(dataAction);
        }

        @Override
        public N2oObject.Operation createOperation(DataRecordRequest request) {

            N2oObject.Operation operation = new N2oObject.Operation();
            operation.setId(TEST_OPERATION);
            operation.setInvocation(new N2oJavaDataProvider());

            return operation;
        }

        @Override
        public List<AbstractParameter> createRegularParams(DataRecordRequest request) {
            return emptyList();
        }

        @Override
        public int getRecordMappingIndex(DataRecordRequest request) {
            return ROW_ARGUMENT_INDEX;
        }
    }
}