package ru.i_novus.ms.rdm.impl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.version.ReferrerVersionCriteria;
import ru.i_novus.ms.rdm.async.api.service.AsyncOperationMessageService;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.service.async.AsyncPublishService;

import java.io.Serializable;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum.PUBLICATION;

@RunWith(MockitoJUnitRunner.class)
public class AsyncPublishServiceTest {

    private static final int REFBOOK_ID = 2;
    private static final String REFBOOK_CODE = "refbook_code";

    private static final int DRAFT_ID = 1;
    private static final String DRAFT_STORAGE_CODE = "draft-storage-code";

    private static final int REFERRER_ID = 12;
    private static final String REFERRER_CODE = "referrer_code";

    private static final int REFERRER_VERSION_ID = 11;
    private static final String REFERRER_STORAGE_CODE = "referrer-storage-code";

    private static final RefBookStatusType FIND_REFERRERS_STATUS = RefBookStatusType.USED;
    private static final RefBookSourceType FIND_REFERRERS_SOURCE = RefBookSourceType.DRAFT;
    private static final ReferrerVersionCriteria FIND_REFERRERS_CRITERIA = new ReferrerVersionCriteria(REFBOOK_CODE, FIND_REFERRERS_STATUS, FIND_REFERRERS_SOURCE);

    @InjectMocks
    private AsyncPublishService service;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private AsyncOperationMessageService asyncOperationMessageService;

    @Test
    public void testPublish() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.getReferenceById(DRAFT_ID)).thenReturn(draftEntity);

        UUID operationId = UUID.randomUUID();
        when(asyncOperationMessageService.send(eq(PUBLICATION), eq(REFBOOK_CODE), any(Serializable[].class)))
                .thenReturn(operationId);

        PublishRequest request = new PublishRequest(draftEntity.getOptLockValue());
        service.publish(DRAFT_ID, request);

        ArgumentCaptor<Serializable> argsCaptor = ArgumentCaptor.forClass(Serializable.class);
        verify(asyncOperationMessageService).send(eq(PUBLICATION), eq(REFBOOK_CODE), (Serializable[]) argsCaptor.capture());

        Serializable[] args = (Serializable[]) argsCaptor.getValue();
        assertEquals(2, args.length);
        assertEquals(DRAFT_ID, args[0]);
        assertEquals(request, args[1]);
    }

    private RefBookEntity createRefBookEntity() {

        final RefBookEntity entity = new DefaultRefBookEntity();
        entity.setCode(REFBOOK_CODE);

        return entity;
    }

    private RefBookVersionEntity createDraftEntity() {

        final RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(createRefBookEntity());

        return entity;
    }
}