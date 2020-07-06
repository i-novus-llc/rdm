package ru.inovus.ms.rdm.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.service.*;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.draft.CreateDraftRequest;
import ru.inovus.ms.rdm.api.model.refbook.RefBookUpdateRequest;
import ru.inovus.ms.rdm.api.model.refdata.UpdateFromFileRequest;
import ru.inovus.ms.rdm.api.model.version.CreateAttributeRequest;
import ru.inovus.ms.rdm.api.model.version.UpdateAttributeRequest;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.api.util.FileNameGenerator;
import ru.inovus.ms.rdm.api.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.api.validation.VersionPeriodPublishValidation;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.file.FileStorage;
import ru.inovus.ms.rdm.impl.repository.RefBookRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.repository.VersionFileRepository;
import ru.inovus.ms.rdm.impl.validation.VersionValidationImpl;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class ArchiveValidationTest {

    private static final Integer refBookId = 1;
    private static final Integer draftId = 2;
    private static final Integer versionId = 3;

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
    private FileStorage fileStorage;

    @Mock
    private FileNameGenerator fileNameGenerator;

    @Mock
    private VersionFileRepository versionFileRepository;
    @Mock
    private VersionNumberStrategy versionNumberStrategy;

    @Mock
    private VersionValidation versionValidation;
    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Before
    public void setUp() {
    }

    @Test
    public void testArchiveValidation() {

        assertArchiveValidationError(() -> draftService.create(new CreateDraftRequest(refBookId, new Structure(), null, Collections.emptyMap())));
        assertArchiveValidationError(() -> draftService.create(refBookId, new FileModel()));

        UpdateFromFileRequest request = new UpdateFromFileRequest(draftId, null, new FileModel());
        assertArchiveValidationError(() -> draftService.updateFromFile(request));

        assertArchiveValidationError(() -> draftService.remove(draftId));
        assertArchiveValidationError(() -> draftService.createAttribute(new CreateAttributeRequest(draftId, null, null, null)));
        assertArchiveValidationError(() -> draftService.updateAttribute(new UpdateAttributeRequest(draftId, null, new Structure.Attribute(), null)));
        assertArchiveValidationError(() -> draftService.deleteAttribute(draftId, null, null));

        RefBookUpdateRequest updateRequest = new RefBookUpdateRequest();
        updateRequest.setVersionId(versionId);
        assertArchiveValidationError(() -> refBookService.update(updateRequest));

    }

    private void assertArchiveValidationError(MethodExecutor executor) {

        doThrow(new NotFoundException(new Message(VersionValidationImpl.REFBOOK_IS_ARCHIVED_EXCEPTION_CODE, refBookId)))
                .when(versionValidation).validateRefBook(eq(refBookId));
        doThrow(new NotFoundException(new Message(VersionValidationImpl.REFBOOK_IS_ARCHIVED_EXCEPTION_CODE, refBookId)))
                .when(versionValidation).validateDraft(eq(draftId));
        doThrow(new NotFoundException(new Message(VersionValidationImpl.REFBOOK_IS_ARCHIVED_EXCEPTION_CODE, refBookId)))
                .when(versionValidation).validateVersion(eq(versionId));

        try {
            executor.execute();
            fail();
        } catch (UserException e) {
            assertEquals("refbook.is.archived", e.getMessage());
        }

        doNothing()
                .when(versionValidation).validateRefBook(eq(refBookId));
        doNothing()
                .when(versionValidation).validateDraft(eq(draftId));
        doNothing()
                .when(versionValidation).validateVersion(eq(versionId));

        try {
            executor.execute();
        } catch (UserException e) {
            assertNotEquals("refbook.is.archived", e.getMessage());
        } catch (Exception ignored){}

    }

    private interface MethodExecutor {
        void execute();
    }

}