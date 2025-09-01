package ru.i_novus.ms.rdm.n2o.l10n.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.l10n.L10nService;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.n2o.l10n.BaseTest;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class L10nLocalizeVersionControllerTest extends BaseTest {

    private static final int TEST_REFBOOK_VERSION_ID = -10;
    private static final int TEST_OPT_LOCK_VALUE = 10;

    private static final String TEST_LOCALE_CODE = "test";

    private static final String TEST_FIELD_CODE = "id";

    @InjectMocks
    private L10nLocalizeVersionController controller;

    @Mock
    private L10nService l10nService;

    @Test
    public void testLocalizeDataRecord() {

        final Map<String, Object> data = new HashMap<>(1);
        data.put(TEST_FIELD_CODE, 10);

        final Row row = new Row(1L, data);

        final LocalizeDataRequest expected =
                new LocalizeDataRequest(TEST_OPT_LOCK_VALUE, TEST_LOCALE_CODE, singletonList(row));

        controller.localizeDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, TEST_LOCALE_CODE, row);

        final ArgumentCaptor<LocalizeDataRequest> captor = ArgumentCaptor.forClass(LocalizeDataRequest.class);
        verify(l10nService, times(1))
                .localizeData(eq(TEST_REFBOOK_VERSION_ID), captor.capture());

        final LocalizeDataRequest actual = captor.getValue();
        assertEquals(expected.getOptLockValue(), actual.getOptLockValue());
        assertEquals(expected.getLocaleCode(), actual.getLocaleCode());
        assertEquals(expected.getRows().size(), actual.getRows().size());

        final Row expectedRow = expected.getRows().getFirst();
        final Row actualRow = actual.getRows().getFirst();
        assertEquals(expectedRow.getSystemId(), actualRow.getSystemId());
        assertEquals(
                expectedRow.getData().get(TEST_FIELD_CODE),
                actualRow.getData().get(TEST_FIELD_CODE)
        );
    }

    @Test
    public void testLocalizeDataRecordFailed() {

        testLocalizeDataRecordFailed(null);

        testLocalizeDataRecordFailed(new Row(1L, null));

        final Map<String, Object> data = new HashMap<>(1);
        final Row row = new Row(1L, data);
        testLocalizeDataRecordFailed(row);

        data.put(TEST_FIELD_CODE, null);
        testLocalizeDataRecordFailed(row);

        data.put(TEST_FIELD_CODE, "");
        testLocalizeDataRecordFailed(row);
    }

    @SuppressWarnings("java:S5778")
    private void testLocalizeDataRecordFailed(Row row) {
        try {
            controller.localizeDataRecord(TEST_REFBOOK_VERSION_ID, TEST_OPT_LOCK_VALUE, TEST_LOCALE_CODE, row);
            fail(getFailedMessage(UserException.class));

        } catch (RuntimeException e) {
            assertEquals(UserException.class, e.getClass());
            assertEquals("data.row.is.empty", getExceptionMessage(e));
        }
    }
}