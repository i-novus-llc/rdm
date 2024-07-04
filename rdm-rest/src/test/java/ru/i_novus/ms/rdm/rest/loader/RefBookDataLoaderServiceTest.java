package ru.i_novus.ms.rdm.rest.loader;

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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.rest.loader.RefBookDataUpdateTypeEnum.SKIP_ON_DRAFT;

@RunWith(MockitoJUnitRunner.class)
public class RefBookDataLoaderServiceTest extends BaseLoaderTest {

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

        final RefBookDataRequest request = createJsonDataRequest(1);

        final boolean actual = service.createAndPublish(request);
        assertFalse(actual);
    }

    @Test
    public void testCreateAndPublishWhenFileModel() {

        final RefBookDataRequest request = createFileDataRequest(1);
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

        final RefBookDataRequest request = createJsonDataRequest(2);

        final Page<RefBook> refBooks = new PageImpl<>(emptyList(), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final boolean actual = service.createOrUpdate(request);
        assertFalse(actual);
    }

    @Test
    public void testCreateOrUpdateWhenUpdateSkip() {

        final RefBookDataRequest request = createJsonDataRequest(2);
        request.setUpdateType(SKIP_ON_DRAFT);

        final RefBook refBook = createRefBook(1);
        final Page<RefBook> refBooks = new PageImpl<>(singletonList(refBook), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final Draft draft = createDraft();
        when(draftService.findDraft(refBook.getCode())).thenReturn(draft);

        final boolean actual = service.createOrUpdate(request);
        assertFalse(actual);
    }
}