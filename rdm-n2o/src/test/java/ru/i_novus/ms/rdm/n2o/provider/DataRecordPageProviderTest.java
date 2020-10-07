package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceComponent;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.global.view.page.N2oSimplePage;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oForm;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oWidget;
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
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@RunWith(MockitoJUnitRunner.class)
public class DataRecordPageProviderTest extends BaseRecordProviderTest {

    @Mock
    protected VersionRestService versionService;
    @InjectMocks
    private DataRecordPageProvider provider;
    @Spy
    private Collection<DataRecordPageResolver> resolvers = new ArrayList<>(1);

    @Before
    public void setUp() {

        DataRecordPageResolver resolver = new TestRecordPageResolver();
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
        assertTrue(metadata instanceof N2oSimplePage);

        N2oSimplePage page = (N2oSimplePage) metadata;
        assertEquals(provider.getCode() + "?" + TEST_CONTEXT, page.getId());

        N2oWidget widget = page.getWidget();
        assertNotNull(widget);
        assertTrue(widget instanceof N2oForm);

        N2oForm form = (N2oForm) widget;
        assertNotNull(form.getQueryId());
        assertNotNull(form.getObjectId());

        List<SourceComponent> items = Arrays.asList(form.getItems());
        assertNotNull(items);
        assertTrue(items.size() >= structure.getAttributes().size());

        structure.getAttributes().forEach(attribute -> {

            final String codeWithPrefix = addPrefix(attribute.getCode());
            assertTrue(existsItem(items, codeWithPrefix));
        });
    }

    private boolean existsItem(List<SourceComponent> items, String id) {

        return items.stream().anyMatch(item -> id.equals(getFieldId(item)));
    }

    private String getFieldId(SourceComponent item) {

        return (item instanceof N2oField) ? ((N2oField) item).getId() : null;
    }

    @Test
    public void testReadOnTransform() {

        testReadOnTransform("{id}_trial");
        testReadOnTransform("{id]_trial");
        testReadOnTransform("[id}_trial");
    }

    private void testReadOnTransform(String context) {

        List<? extends SourceMetadata> list = provider.read(context);
        assertNotNull(list);
        assertEquals(1, list.size());

        SourceMetadata metadata = list.get(0);
        assertTrue(metadata instanceof N2oSimplePage);

        N2oSimplePage page = (N2oSimplePage) metadata;
        assertNull(page.getId());
        assertNull(page.getWidget());
    }

    @Test
    public void testGetMetadataClasses() {

        Collection<Class<? extends SourceMetadata>> list = provider.getMetadataClasses();
        assertNotNull(list);
    }

    private static class TestRecordPageResolver implements DataRecordPageResolver {

        @Override
        public boolean isSatisfied(String dataAction) {
            return TEST_ACTION.equals(dataAction);
        }

        @Override
        public List<SourceComponent> createRegularFields(DataRecordRequest request) {
            return emptyList();
        }

        @Override
        public void processDynamicFields(DataRecordRequest request, List<SourceComponent> list) {
            // Nothing to do.
        }
    }
}