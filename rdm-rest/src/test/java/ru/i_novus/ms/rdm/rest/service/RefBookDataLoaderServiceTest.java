package ru.i_novus.ms.rdm.rest.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.rest.BaseTest;
import ru.i_novus.ms.rdm.rest.loader.RefBookDataRequest;

import java.util.HashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RefBookDataLoaderServiceTest extends BaseTest {

    protected static final int REFBOOK_ID = 1;
    protected static final int DRAFT_ID = 2;

    private static final String LOADED_CODE = "LOADED_DATA_";
    private static final String LOADED_NAME = "Loaded Name ";
    private static final String LOADED_STRUCTURE = "{}";
    private static final String LOADED_DATA = "{}";

    private static final String LOADED_FILE_NAME = "loadedData_";
    private static final String LOADED_FILE_EXT = ".xml";
    private static final String LOADED_FILE_FOLDER = "src/test/resources/" + "testLoader/";

    @InjectMocks
    private RefBookDataLoaderService service;

    @Mock
    private RefBookService refBookService;

    @Mock
    private DraftService draftService;

    @Mock
    private PublishService publishService;

    @Test
    public void testCreateAndPublish() {

        final RefBookDataRequest request = createRequest();

        final boolean actual = service.createAndPublish(request);
        assertFalse(actual);
    }

    @Test
    public void testCreateAndPublishWhenFileModel() {

        final RefBookDataRequest request = createRequestWithFileModel();
        final FileModel fileModel = request.getFileModel();

        final Draft draft = createDraft();
        when(refBookService.create(fileModel)).thenReturn(draft);

        final boolean actual = service.createAndPublish(request);
        assertTrue(actual);

        verify(refBookService).create(fileModel);

        verify(publishService).publish(eq(draft.getId()), any());

        verifyNoMoreInteractions(refBookService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenCreate() {

        final RefBookDataRequest request = createRequest();

        final Page<RefBook> refBooks = new PageImpl<>(emptyList(), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final boolean actual = service.createOrUpdate(request);
        assertFalse(actual);
    }

    @Test
    public void testCreateOrUpdateWhenUpdateSkip() {

        final RefBookDataRequest request = createRequest();

        final RefBook refBook = createRefBook();
        final Page<RefBook> refBooks = new PageImpl<>(singletonList(refBook), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final Draft draft = createDraft();
        when(draftService.findDraft(refBook.getCode())).thenReturn(draft);

        final boolean actual = service.createOrUpdate(request);
        assertFalse(actual);
    }

    private RefBookDataRequest createRequest() {

        final RefBookDataRequest result = new RefBookDataRequest();
        result.setChangeSetId("id");

        result.setCode(LOADED_CODE);

        result.setPassport(new HashMap<>());
        result.getPassport().put("name", LOADED_NAME);

        result.setStructure(LOADED_STRUCTURE);
        result.setData(LOADED_DATA);

        return result;
    }

    private RefBookDataRequest createRequestWithFileModel() {

        final RefBookDataRequest result = new RefBookDataRequest();
        result.setChangeSetId("id");

        result.setCode(LOADED_CODE);

        result.setPassport(new HashMap<>());
        result.getPassport().put("name", LOADED_NAME);

        final String fileName = getFileName(0);
        final FileModel fileModel = new FileModel(LOADED_FILE_FOLDER, fileName);
        result.setFileModel(fileModel);

        return result;
    }

    private String getFileName(int index) {
        return String.format("%s%d%s", LOADED_FILE_NAME, index, LOADED_FILE_EXT);
    }

    private RefBook createRefBook() {

        final RefBook result = new RefBook();
        result.setRefBookId(REFBOOK_ID);
        result.setCode(LOADED_CODE);

        return result;
    }

    private Draft createDraft() {

        final Draft result = new Draft();
        result.setId(DRAFT_ID);

        return result;
    }
}