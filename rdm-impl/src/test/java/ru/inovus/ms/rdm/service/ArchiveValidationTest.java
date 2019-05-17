package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.UserException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.repositiory.VersionFileRepository;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.FileNameGenerator;
import ru.inovus.ms.rdm.util.VersionNumberStrategy;
import ru.inovus.ms.rdm.util.VersionPeriodPublishValidation;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

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
    private VersionService versionService;

    @Mock
    private FieldFactory fieldFactory;

    @Mock
    private DropDataService dropDataService;

    @Mock
    private RefBookRepository refBookRepository;

    @Mock
    private SearchDataService searchDataService;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileNameGenerator fileNameGenerator;

    @Mock
    private VersionFileRepository versionFileRepository;

    @Mock
    private VersionNumberStrategy versionNumberStrategy;

    @Mock
    private VersionPeriodPublishValidation versionPeriodPublishValidation;

    @Before
    public void setUp() {
        when(versionRepository.exists(eq(isVersionOfRefBook(refBookId)))).thenReturn(true);
        when(versionRepository.exists(eq(hasVersionId(draftId).and(isDraft())))).thenReturn(true);
        when(versionRepository.exists(eq(versionId))).thenReturn(true);
        when(refBookRepository.existsByCode(any())).thenReturn(false);
    }

    @Test
    public void testArchiveValidation() {

        assertArchiveValidationError(() -> draftService.create(new CreateDraftRequest(refBookId, new Structure(), null)));
        assertArchiveValidationError(() -> draftService.create(refBookId, new FileModel()));
        assertArchiveValidationError(() -> draftService.updateData(draftId, new FileModel()));
        assertArchiveValidationError(() -> draftService.remove(draftId));
        assertArchiveValidationError(() -> draftService.createAttribute(new CreateAttribute(draftId, null, null)));
        assertArchiveValidationError(() -> draftService.updateAttribute(new UpdateAttribute(draftId, new Structure.Attribute(), null)));
        assertArchiveValidationError(() -> draftService.deleteAttribute(draftId, null));

        RefBookUpdateRequest updateRequest = new RefBookUpdateRequest();
        updateRequest.setVersionId(versionId);
        assertArchiveValidationError(() -> refBookService.update(updateRequest));

    }

    private void assertArchiveValidationError(MethodExecutor executor) {

        when(versionRepository.exists(eq(hasVersionId(draftId).and(isArchived())))).thenReturn(true);
        when(versionRepository.exists(eq(isVersionOfRefBook(refBookId).and(isArchived())))).thenReturn(true);
        when(versionRepository.exists(eq(hasVersionId(versionId).and(isArchived())))).thenReturn(true);
        when(versionRepository.exists(eq(isVersionOfRefBook(versionId).and(isArchived())))).thenReturn(true);
        try {
            executor.execute();
            fail();
        } catch (UserException e) {
            assertEquals("refbook.is.archived", e.getMessage());
        }

        when(versionRepository.exists(eq(hasVersionId(draftId).and(isArchived())))).thenReturn(false);
        when(versionRepository.exists(eq(isVersionOfRefBook(refBookId).and(isArchived())))).thenReturn(false);
        when(versionRepository.exists(eq(hasVersionId(versionId).and(isArchived())))).thenReturn(false);
        when(versionRepository.exists(eq(isVersionOfRefBook(versionId).and(isArchived())))).thenReturn(false);
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