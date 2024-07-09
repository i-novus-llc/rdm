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
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataRequest;
import ru.i_novus.ms.rdm.api.model.loader.RefBookDataResponse;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.service.DraftService;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.impl.repository.loader.RefBookDataLoadLogRepository;

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

    @Mock
    private RefBookDataLoadLogRepository repository;

    @Test
    public void testCreateAndPublishWithJson() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 1);

        final RefBookDataResponse actual = service.load(request);
        assertNull(actual);
    }

    @Test
    public void testCreateAndPublishWithFile() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID);

        final String refBookCode = request.getCode();
        final FileModel fileModel = request.getFileModel();

        final Draft draft = createDraft(DRAFT_ID);
        when(refBookService.create(fileModel)).thenReturn(draft);

        final PublishResponse publishResponse = createPublishResponse(request);
        when(publishService.publish(eq(draft.getId()), any())).thenReturn(publishResponse);

        when(refBookService.getId(refBookCode)).thenReturn(REFBOOK_ID);

        final RefBookDataResponse actual = service.load(request);
        assertNotNull(actual);
        assertEquals(REFBOOK_ID, actual.getRefBookId().intValue());

        final ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(publishService, times(1)).publish(eq(draft.getId()), captor.capture());

        final PublishRequest actualPublishRequest = captor.getValue();
        assertNotNull(actualPublishRequest);
        assertEquals(actualPublishRequest.getFromDate(), actual.getExecutedDate());
    }

    @Test
    public void testCreateOrUpdateWhenCreate() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 2);

        mockExistsRefBook(request);

        final RefBookDataResponse actual = service.load(request);
        assertNull(actual);

        verify(repository).existsByCodeAndChangeSetId(request.getCode(), request.getChangeSetId());

        verifyNoMoreInteractions(refBookService, draftService, publishService, repository);
    }

    @Test
    public void testCreateOrUpdateWhenCreateOnly() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID + 3);
        request.setUpdateType(CREATE_ONLY);

        mockExistsRefBook(request);

        final RefBookDataResponse actual = service.load(request);
        assertNull(actual);

        verifyNoMoreInteractions(refBookService, draftService, publishService);
    }

    @Test
    public void testCreateOrUpdateWhenSkipOnDraft() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID);
        request.setUpdateType(SKIP_ON_DRAFT);

        final String refBookCode = request.getCode();

        mockExistsRefBook(request);
        mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBookCode)).thenReturn(draft);

        final RefBookDataResponse actual = service.load(request);
        assertNull(actual);

        verify(repository).existsByCodeAndChangeSetId(refBookCode, request.getChangeSetId());
        verify(refBookService).search(any());
        verify(draftService).findDraft(refBookCode);

        verifyNoMoreInteractions(refBookService, draftService, publishService, repository);
    }

    @Test
    public void testCreateOrUpdateWhenForceUpdateWithJson() {

        final RefBookDataRequest request = createJsonDataRequest(REFBOOK_ID);
        request.setUpdateType(FORCE_UPDATE);

        final String refBookCode = request.getCode();

        mockExistsRefBook(request);
        mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBookCode)).thenReturn(draft);

        final RefBookDataResponse actual = service.load(request);
        assertNull(actual);

        verify(repository).existsByCodeAndChangeSetId(refBookCode, request.getChangeSetId());
        verify(refBookService).search(any());
        verify(draftService).findDraft(refBookCode);

        verifyNoMoreInteractions(refBookService, draftService, publishService, repository);
    }

    @Test
    public void testCreateOrUpdateWhenForceUpdateWithFile() {

        final RefBookDataRequest request = createFileDataRequest(REFBOOK_ID);
        request.setUpdateType(FORCE_UPDATE);

        final String refBookCode = request.getCode();
        final FileModel fileModel = request.getFileModel();

        mockExistsRefBook(request);
        mockSearchRefBooks(REFBOOK_ID);

        final Draft draft = createDraft(DRAFT_ID);
        when(draftService.findDraft(refBookCode)).thenReturn(draft);

        when(draftService.create(REFBOOK_ID, fileModel)).thenReturn(draft);

        final PublishResponse publishResponse = createPublishResponse(request);
        when(publishService.publish(eq(draft.getId()), any())).thenReturn(publishResponse);

        when(refBookService.getId(refBookCode)).thenReturn(REFBOOK_ID);

        final RefBookDataResponse actual = service.load(request);
        assertNotNull(actual);
        assertEquals(REFBOOK_ID, actual.getRefBookId().intValue());

        final ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(publishService, times(1)).publish(eq(draft.getId()), captor.capture());

        final PublishRequest actualPublishRequest = captor.getValue();
        assertNotNull(actualPublishRequest);
        assertEquals(actualPublishRequest.getFromDate(), actual.getExecutedDate());
    }

    private void mockExistsRefBook(RefBookDataRequest request) {

        when(repository.existsByCodeAndChangeSetId(request.getCode(), request.getChangeSetId()))
                .thenReturn(false);
    }

    @SuppressWarnings("UnusedReturnValue")
    private RefBook mockSearchRefBooks(int index) {

        final RefBook refBook = createRefBook(index);
        final Page<RefBook> refBooks = new PageImpl<>(singletonList(refBook), new RefBookCriteria(), 0);
        when(refBookService.search(any())).thenReturn(refBooks);

        return refBook;
    }

    private static PublishResponse createPublishResponse(RefBookDataRequest request) {

        final PublishResponse publishResponse = new PublishResponse();
        publishResponse.setRefBookCode(request.getCode());
        publishResponse.setOldId(DRAFT_ID);
        publishResponse.setNewId(VERSION_ID);

        return publishResponse;
    }
}