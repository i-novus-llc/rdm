package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateFromFileRequest;
import ru.i_novus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.DeleteAttributeRequest;
import ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.BaseTest;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.version.ValidateVersionNotArchivedStrategy;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveValidationTest extends BaseTest {

    private static final Integer REFBOOK_ID = -10;
    private static final Integer VERSION_ID = 2;
    private static final Integer DRAFT_ID = 6;

    @InjectMocks
    private RefBookServiceImpl refBookService;
    @InjectMocks
    private DraftServiceImpl draftService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private DraftDataService draftDataService;
    @Mock
    private DropDataService dropDataService;
    @Mock
    private SearchDataService searchDataService;

    @Mock
    private RefBookRepository refBookRepository;
    @Mock
    private VersionService versionService;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private VersionFileRepository versionFileRepository;
    @Mock
    private VersionNumberStrategy versionNumberStrategy;

    @Mock
    private VersionValidation versionValidation;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Mock
    private ValidateVersionNotArchivedStrategy validateVersionNotArchivedStrategy;

    @Before
    public void setUp() {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());

        setField(refBookService, "strategyLocator", strategyLocator);
        setField(draftService, "strategyLocator", strategyLocator);
    }

    @Test
    public void testArchiveValidation() {

        assertArchiveValidationError(() -> draftService.create(new CreateDraftRequest(REFBOOK_ID, new Structure(), null, Collections.emptyMap())));
        assertArchiveValidationError(() -> draftService.create(REFBOOK_ID, new FileModel()));

        UpdateFromFileRequest request = new UpdateFromFileRequest(null, new FileModel());
        assertArchiveValidationError(() -> draftService.updateFromFile(DRAFT_ID, request));

        assertArchiveValidationError(() -> draftService.remove(DRAFT_ID));
        assertArchiveValidationError(() -> draftService.updateAttribute(DRAFT_ID, new UpdateAttributeRequest(null, new Structure.Attribute(), null)));
        assertArchiveValidationError(() -> draftService.deleteAttribute(DRAFT_ID, new DeleteAttributeRequest(null, null)));
        assertArchiveValidationError(() -> draftService.createAttribute(DRAFT_ID, new CreateAttributeRequest(null, null, null)));

        RefBookUpdateRequest updateRequest = new RefBookUpdateRequest();
        updateRequest.setVersionId(VERSION_ID);
        assertArchiveValidationError(() -> refBookService.update(updateRequest));
    }

    private void assertArchiveValidationError(MethodExecutor executor) {

        doThrow(new NotFoundException(new Message(VersionValidationImpl.REFBOOK_IS_ARCHIVED_EXCEPTION_CODE, REFBOOK_ID)))
                .when(versionValidation).validateRefBook(eq(REFBOOK_ID));

        RefBookVersionEntity versionEntity = createVersionEntity();
        when(versionRepository.findById(VERSION_ID)).thenReturn(Optional.of(versionEntity));
        RefBookVersionEntity draftEntity = createDraftEntity();
        when(versionRepository.findById(DRAFT_ID)).thenReturn(Optional.of(draftEntity));

        doThrow(new NotFoundException(new Message(VersionValidationImpl.REFBOOK_IS_ARCHIVED_EXCEPTION_CODE, REFBOOK_ID)))
                .when(validateVersionNotArchivedStrategy).validate(any());

        try {
            executor.execute();
            fail();

        } catch (UserException e) {
            assertEquals("refbook.is.archived", e.getMessage());
        }

        doNothing()
                .when(versionValidation).validateRefBook(eq(REFBOOK_ID));

        doNothing()
                .when(validateVersionNotArchivedStrategy).validate(any());

        try {
            executor.execute();

        } catch (UserException e) {
            assertNotEquals("refbook.is.archived", e.getMessage());

        } catch (Exception ignored){}
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(VERSION_ID);

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        entity.setRefBook(refBookEntity);

        return entity;
    }

    private RefBookVersionEntity createDraftEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(DRAFT_ID);
        entity.setStatus(RefBookVersionStatus.DRAFT);

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        entity.setRefBook(refBookEntity);

        return entity;
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        Map<Class<? extends Strategy>, Strategy> result = new HashMap<>();
        result.put(ValidateVersionNotArchivedStrategy.class, validateVersionNotArchivedStrategy);

        return result;
    }
}