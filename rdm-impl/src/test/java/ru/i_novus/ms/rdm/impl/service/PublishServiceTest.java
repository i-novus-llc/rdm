package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.version.ReferrerVersionCriteria;
import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.async.AsyncOperationQueue;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.publish.BasePublishStrategy;
import ru.i_novus.ms.rdm.impl.utils.ReflectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.io.Serializable;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PublishServiceTest {

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
    private PublishServiceImpl service;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;

    @Mock
    private ReferenceService referenceService;

    @Mock
    private VersionValidation versionValidation;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private BasePublishStrategy basePublishStrategy;

    @Mock
    private AsyncOperationQueue asyncQueue;

    @Before
    public void setUp() throws NoSuchFieldException {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        ReflectionUtils.setField(service, PublishServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);
    }

    @Test
    public void testPublish() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftEntity));

        PublishRequest request = new PublishRequest(draftEntity.getOptLockValue());
        PublishResponse result = createPublishResponse();

        when(basePublishStrategy.publish(draftEntity, request)).thenReturn(result);

        service.publish(DRAFT_ID, request);

        verifyBasePublish(draftEntity, request);

        verifyNoMoreInteractions(basePublishStrategy);
    }

    @Test
    public void testPublishWithRefresh() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftEntity));

        PublishRequest request = new PublishRequest(draftEntity.getOptLockValue());
        request.setResolveConflicts(true);
        PublishResponse result = createPublishResponse();

        when(basePublishStrategy.publish(draftEntity, request)).thenReturn(result);

        RefBookVersionEntity referrer = createReferrerVersionEntity();
        List<RefBookVersionEntity> referrers = singletonList(referrer);
        when(versionRepository.findReferrerVersions(
                eq(REFBOOK_CODE),
                eq(FIND_REFERRERS_STATUS.name()),
                eq(FIND_REFERRERS_SOURCE.name()),
                any(PageRequest.class))
        )
                .thenReturn(new PageImpl<>(referrers, FIND_REFERRERS_CRITERIA, 1)) // referrers
                .thenReturn(new PageImpl<>(emptyList(), FIND_REFERRERS_CRITERIA, 1)); // stop

        when(conflictRepository.existsByReferrerVersionIdAndPublishedVersionId(REFERRER_VERSION_ID, DRAFT_ID))
                .thenReturn(false);

        when(versionRepository.findById(REFERRER_VERSION_ID)).thenReturn(Optional.of(referrer));

        when(basePublishStrategy.publish(eq(referrer), any(PublishRequest.class))).thenReturn(null);

        service.publish(DRAFT_ID, request);

        verifyBasePublish(draftEntity, request);
        verify(basePublishStrategy).publish(eq(referrer), any(PublishRequest.class));

        verify(referenceService).refreshLastReferrers(REFBOOK_CODE);

        verifyNoMoreInteractions(basePublishStrategy);
    }

    @Test
    public void testPublishWhenInvalidId() {

        Integer draftId = 0;
        PublishRequest request = new PublishRequest();
        try {
            service.publish(draftId, request);
            fail();

        } catch (UserException e) {
            assertEquals("draft.not.found", e.getCode());
            assertEquals(draftId, e.getArgs()[0]);
        }
    }

    @Test
    public void testPublishWhenNullResponse() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftEntity));

        PublishRequest request = new PublishRequest(draftEntity.getOptLockValue());
        service.publish(DRAFT_ID, request);

        verifyBasePublish(draftEntity, request);

        verifyNoMoreInteractions(basePublishStrategy);
    }

    @Test
    public void testPublishAsync() {

        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.getOne(DRAFT_ID)).thenReturn(draftEntity);

        UUID operationId = UUID.randomUUID();
        when(asyncQueue.send(eq(AsyncOperationTypeEnum.PUBLICATION), eq(REFBOOK_CODE), any(Serializable[].class)))
                .thenReturn(operationId);

        PublishRequest request = new PublishRequest(draftEntity.getOptLockValue());
        UUID result = service.publishAsync(DRAFT_ID, request);
        assertSame(operationId, result);

        ArgumentCaptor<Serializable> argsCaptor = ArgumentCaptor.forClass(Serializable.class);
        verify(asyncQueue).send(eq(AsyncOperationTypeEnum.PUBLICATION), eq(REFBOOK_CODE), (Serializable[]) argsCaptor.capture());

        Serializable[] args = (Serializable[]) argsCaptor.getValue();
        assertEquals(2, args.length);
        assertEquals(DRAFT_ID, args[0]);
        assertEquals(request, args[1]);
    }

    private void verifyBasePublish(RefBookVersionEntity entity, PublishRequest request) {

        ArgumentCaptor<RefBookVersionEntity> captor = ArgumentCaptor.forClass(RefBookVersionEntity.class);
        verify(basePublishStrategy).publish(captor.capture(), eq(request));
        assertSame(entity, captor.getValue());
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REFBOOK_CODE);

        return entity;
    }

    private RefBookVersionEntity createDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setStorageCode(DRAFT_STORAGE_CODE);
        entity.setRefBook(createRefBookEntity());
        entity.setStatus(RefBookVersionStatus.DRAFT);
        entity.setStructure(createStructure());

        return entity;
    }

    private Structure createStructure() {

        return new Structure(
                asList(
                        Structure.Attribute.build("Code", "Код", FieldType.INTEGER, "Код записи"),
                        Structure.Attribute.build("Name", "Имя", FieldType.STRING, "Наименование"),
                        Structure.Attribute.build("Rec_Date", "Дата", FieldType.DATE, "Дата записи"),
                        Structure.Attribute.build("Count", "Счётчик", FieldType.FLOAT, "Количество")
                ),
                null);
    }

    private PublishResponse createPublishResponse() {

        PublishResponse result = new PublishResponse();
        result.setRefBookCode(REFBOOK_CODE);
        result.setNewId(DRAFT_ID);

        return result;
    }

    private RefBookEntity createReferrerEntity() {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(REFERRER_ID);
        entity.setCode(REFERRER_CODE);

        return entity;
    }

    private RefBookVersionEntity createReferrerVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(REFERRER_VERSION_ID);
        entity.setStorageCode(REFERRER_STORAGE_CODE);
        entity.setRefBook(createReferrerEntity());
        entity.setStatus(RefBookVersionStatus.PUBLISHED);

        return entity;
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        // RefBook:
        result.put(BasePublishStrategy.class, basePublishStrategy);

        return result;
    }
}