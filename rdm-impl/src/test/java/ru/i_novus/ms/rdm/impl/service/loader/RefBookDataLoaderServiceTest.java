package ru.i_novus.ms.rdm.impl.service.loader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static ru.i_novus.ms.rdm.api.model.loader.RefBookDataUpdateTypeEnum.*;

@RunWith(MockitoJUnitRunner.class)
public class RefBookDataLoaderServiceTest extends BaseLoaderTest {

    @InjectMocks
    private RefBookDataLoaderServiceImpl service;

    @Mock
    private RefBookService refBookService;

    @Mock
    private DraftService draftService;

    @Mock
    private PublishService publishService;

    @Test
    public void testCreateAndPublishWithJson() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 1);

        final RefBookDataResponse actual = service.createAndPublish(request);
        assertNull(actual);
    }

    @Test
    public void testCreateAndPublishWithFile() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID);
        final FileModel fileModel = request.getFileModel();

        final Draft draft = createDraft(DRAFT_ID);
        when(refBookService.create(fileModel)).thenReturn(draft);

        final RefBookDataResponse actual = service.createAndPublish(request);
        assertNotNull(actual);

        verify(refBookService).create(fileModel);

        final ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(publishService, times(1)).publish(eq(draft.getId()), captor.capture());

        final PublishRequest expectedPublishRequest = new PublishRequest();
        final PublishRequest actualPublishRequest = captor.getValue();
        assertNotNull(actualPublishRequest);
        assertEquals(expectedPublishRequest.toString(), actualPublishRequest.toString());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenCreate() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 2);

        final Page<RefBook> refBooks = new PageImpl<>(emptyList(), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final RefBookDataResponse actual = service.createOrUpdate(request);
        assertNull(actual);

        verify(refBookService).search(any());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenCreateOnly() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 3);
        request.setUpdateType(CREATE_ONLY);

        final Page<RefBook> refBooks = new PageImpl<>(emptyList(), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        final RefBookDataResponse actual = service.createOrUpdate(request);
        assertNull(actual);

        verify(refBookService).search(any());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenSkipOnDraft() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID);
        request.setUpdateType(SKIP_ON_DRAFT);

        final RefBook refBook = mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBook.getCode())).thenReturn(draft);

        final RefBookDataResponse actual = service.createOrUpdate(request);
        assertNull(actual);

        verify(refBookService).search(any());
        verify(draftService).findDraft(refBook.getCode());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenForceUpdateWithJson() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID);
        request.setUpdateType(FORCE_UPDATE);

        final RefBook refBook = mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBook.getCode())).thenReturn(draft);

        final RefBookDataResponse actual = service.createOrUpdate(request);
        assertNull(actual);

        verify(refBookService).search(any());
        verify(draftService).findDraft(refBook.getCode());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenForceUpdateWithFile() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID);
        request.setUpdateType(FORCE_UPDATE);
        final FileModel fileModel = request.getFileModel();

        final RefBook refBook = mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBook.getCode())).thenReturn(draft);

        when(draftService.create(REFBOOK_ID, fileModel)).thenReturn(draft);

        final RefBookDataResponse actual = service.createOrUpdate(request);
        assertNotNull(actual);

        verify(refBookService).search(any());
        verify(draftService).findDraft(refBook.getCode());
        verify(draftService).create(REFBOOK_ID, fileModel);
        verify(publishService).publish(eq(draft.getId()), any());

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    private RefBook mockSearchRefBooks(int index) {

        final RefBook refBook = createRefBook(index);
        final Page<RefBook> refBooks = new PageImpl<>(singletonList(refBook), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        return refBook;
    }
}