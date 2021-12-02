package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@RunWith(MockitoJUnitRunner.class)
public class PublishServiceTest {

    @InjectMocks
    private PublishServiceImpl service;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private BasePublishService basePublishService;
    @Mock
    private ReferenceService referenceService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private AsyncOperationQueue asyncQueue;

    @Test
    public void testPublish() {

        // to-do.
    }

    @Test
    public void testPublishWhenInvalidId() {

        Integer draftId = 0;
        try {
            service.publish(draftId, new PublishRequest());
            fail();

        } catch (UserException e) {
            assertEquals("draft.not.found", e.getCode());
            assertEquals(draftId, e.getArgs()[0]);
        }
    }
}