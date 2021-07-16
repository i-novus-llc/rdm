package ru.i_novus.ms.rdm.impl.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookConflictQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceServiceTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final String REFBOOK_CODE = "test";
    private static final Integer VERSION_ID = 2;

    private static final Integer DRAFT_ID = 6;
    private static final String DRAFT_CODE = "draft_code";

    private static final Integer REFERRED_BOOK_ID = -20;

    @InjectMocks
    private ReferenceServiceImpl referenceService;

    @Mock
    private RefBookVersionRepository versionRepository;
    @Mock
    private RefBookConflictRepository conflictRepository;
    @Mock
    private RefBookConflictQueryProvider conflictQueryProvider;

    @Mock
    private DraftDataService draftDataService;

    @Mock
    private DraftService draftService;

    @Mock
    private VersionValidation versionValidation;

    @Test
    public void testRefreshReferrerWithoutReferences() {

        RefBookVersionEntity draftEntity = createDraftEntity(createRefBookEntity());
        when(versionRepository.getOne(DRAFT_ID)).thenReturn(draftEntity);

        referenceService.refreshReferrer(DRAFT_ID, null);

        verify(versionValidation).validateVersionExists(DRAFT_ID);
        verify(versionRepository).getOne(DRAFT_ID);
        verify(versionValidation).validateOptLockValue(eq(DRAFT_ID), any(), any());

        verifyNoMoreInteractions(versionRepository, versionValidation);
    }

    private RefBookVersionEntity createDraftEntity(RefBookEntity refBookEntity) {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setRefBook(refBookEntity);
        entity.setStructure(new Structure());
        entity.setStorageCode(DRAFT_CODE);
        entity.setStatus(RefBookVersionStatus.DRAFT);

        return entity;
    }

    private RefBookEntity createRefBookEntity() {

        return createRefBookEntity(REFBOOK_ID, REFBOOK_CODE);
    }

    private RefBookEntity createRefBookEntity(Integer id, String code) {

        RefBookEntity entity = new DefaultRefBookEntity();
        entity.setId(id);
        entity.setCode(code);

        return entity;
    }
}