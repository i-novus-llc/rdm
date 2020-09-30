package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

import static org.junit.Assert.*;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.FIELD_LOCALE_NAME;

@RunWith(MockitoJUnitRunner.class)
public class L10nLocalizeRecordQueryResolverTest {

    private static final String TEST_UNSATISFIED_ACTION = "ab";

    @InjectMocks
    private L10nLocalizeRecordQueryResolver resolver;

    @Test
    public void testIsSatisfied() {

        assertTrue(resolver.isSatisfied(DATA_ACTION_LOCALIZE));
        assertFalse(resolver.isSatisfied(TEST_UNSATISFIED_ACTION));
        assertFalse(resolver.isSatisfied(null));
    }

    @Test
    public void testCreateRegularFields() {

        DataRecordRequest request = new DataRecordRequest();

        List<N2oQuery.Field> fields = resolver.createRegularFields(request);
        assertNotNull(fields);
        assertEquals(1, fields.size());
        assertEquals(FIELD_LOCALE_NAME, fields.get(0).getId());
    }
}