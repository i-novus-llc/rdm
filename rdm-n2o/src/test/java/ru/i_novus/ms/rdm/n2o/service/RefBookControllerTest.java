package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.RdmPermission;
import ru.i_novus.ms.rdm.n2o.criteria.RefBookStatusCriteria;
import ru.i_novus.ms.rdm.n2o.criteria.RefBookTypeCriteria;
import ru.i_novus.ms.rdm.n2o.criteria.UiRefBookCriteria;
import ru.i_novus.ms.rdm.n2o.model.RefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBook;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookStatus;
import ru.i_novus.ms.rdm.n2o.model.UiRefBookType;
import ru.i_novus.ms.rdm.n2o.util.RefBookAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RefBookControllerTest {

    private static final int REFBOOK_ID = 1;
    private static final String REF_BOOK_CODE = "test_refbook";
    private static final int DRAFT_ID = 2;
    private static final int VERSION_ID = 3;

    private static final RefBookTypeEnum REFBOOK_TYPE_ID = RefBookTypeEnum.DEFAULT;
    private static final String REFBOOK_TYPE_NAME = "refbook.type.default";

    @InjectMocks
    private RefBookController controller;

    @Mock
    private RefBookService refBookService;

    @Mock
    private RefBookAdapter refBookAdapter;

    @Mock
    private Messages messages;

    @Mock
    private RdmPermission rdmPermission;

    @Test
    public void testGetList() {

        final RefBook refBook = createRefBook();
        final List<RefBook> refBooks = singletonList(refBook);

        final UiRefBookCriteria uiCriteria = new UiRefBookCriteria();
        final RefBookCriteria criteria = new RefBookCriteria(uiCriteria);
        when(refBookService.search(eq(criteria)))
                .thenReturn(new PageImpl<>(refBooks, criteria, refBooks.size()));

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        final Page<UiRefBook> page = controller.getList(uiCriteria);
        assertNotNull(page.getContent());
        assertEquals(refBooks.size(), page.getTotalElements());

        final UiRefBook actual = page.getContent().get(0);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testGetVersionRefBook() {

        final RefBook refBook = createRefBook();
        when(refBookService.getByVersionId(VERSION_ID)).thenReturn(refBook);

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        final UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setVersionId(VERSION_ID);

        final UiRefBook actual = controller.getVersionRefBook(criteria);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testGetVersionRefBookWhenExcludeDraft() {

        final RefBook refBook = createRefBook();
        when(refBookService.getByVersionId(VERSION_ID)).thenReturn(refBook);

        final UiRefBookCriteria criteria = new UiRefBookCriteria();
        criteria.setVersionId(VERSION_ID);

        when(rdmPermission.excludeDraft()).thenReturn(true);

        final RefBook changed = new RefBook(refBook);
        changed.setDraftVersionId(null);
        when(refBookAdapter.toUiRefBook(eq(changed))).thenReturn(new UiRefBook(changed));

        final UiRefBook actual = controller.getVersionRefBook(criteria);
        assertRefBookEquals(refBook, actual);

        assertNull(actual.getDraftVersionId());
        assertNull(changed.getDraftVersionId());
    }

    @Test
    public void testGetLastVersion() {

        final RefBook refBook = createRefBook();
        final RefBook oldBook = createRefBook();
        oldBook.setId(VERSION_ID + 1);
        final List<RefBook> refBooks = List.of(refBook, oldBook);

        final UiRefBookCriteria uiCriteria = new UiRefBookCriteria();
        final RefBookCriteria criteria = new RefBookCriteria(uiCriteria);
        when(refBookService.searchVersions(eq(criteria)))
                .thenReturn(new PageImpl<>(refBooks, criteria, refBooks.size()));

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        final UiRefBook actual = controller.getLastVersion(uiCriteria);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testSearchReferenceRefBooks() {

        final RefBook refBook = createRefBook();
        final List<RefBook> refBooks = new ArrayList<>(1);
        refBooks.add(refBook);

        ArgumentCaptor<RefBookCriteria> captor = ArgumentCaptor.forClass(RefBookCriteria.class);
        when(refBookService.search(captor.capture())).thenAnswer(v ->
                new PageImpl<>(refBooks, (RefBookCriteria) v.getArguments()[0], 1)
        );

        final UiRefBookCriteria criteria = new UiRefBookCriteria();
        final Page<UiRefBook> page = controller.searchReferenceRefBooks(criteria);
        assertNotNull(page.getContent());
        assertEquals(refBooks.size(), page.getTotalElements());

        final RefBookCriteria captured = captor.getValue();
        assertTrue(captured.getHasPublished());
        assertTrue(captured.getExcludeDraft());
        assertEquals(RefBookSourceType.LAST_PUBLISHED, captured.getSourceType());
    }

    @Test
    public void testGetTypeList() {

        final Page<UiRefBookType> page = controller.getTypeList();
        assertNotNull(page.getContent());
        assertEquals(RefBookTypeEnum.values().length, page.getTotalElements());
    }

    @Test
    public void testGetTypeItem() {

        when(messages.getMessage(any(String.class))).thenAnswer(v -> v.getArguments()[0]);

        final RefBookTypeCriteria criteria = new RefBookTypeCriteria();
        criteria.setId(REFBOOK_TYPE_ID);

        final UiRefBookType item = controller.getTypeItem(criteria);
        assertNotNull(item);
        assertEquals(criteria.getId(), item.getId());
        assertEquals(REFBOOK_TYPE_NAME, item.getName());
    }

    @Test
    public void testGetTypeItemByName() {

        when(messages.getMessage(any(String.class))).thenAnswer(v -> v.getArguments()[0]);

        final RefBookTypeCriteria criteria = new RefBookTypeCriteria();
        criteria.setName(REFBOOK_TYPE_NAME);

        final UiRefBookType item = controller.getTypeItem(criteria);
        assertNotNull(item);
        assertEquals(REFBOOK_TYPE_ID, item.getId());
        assertEquals(criteria.getName(), item.getName());
    }

    @Test
    public void testGetStatusList() {

        final Page<UiRefBookStatus> page = controller.getStatusList(new RefBookStatusCriteria());
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length, page.getTotalElements());
    }

    @Test
    public void testGetStatusListWithoutDraft() {

        final RefBookStatusCriteria criteria = new RefBookStatusCriteria();
        criteria.setExcludeDraft(true);

        final Page<UiRefBookStatus> page = controller.getStatusList(criteria);
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length - 1, page.getTotalElements());
        assertTrue(page.getContent().stream().noneMatch(status -> Objects.equals(RefBookStatus.HAS_DRAFT, status.getId())));
    }

    @Test
    public void testGetStatusListWithoutArchived() {

        final RefBookStatusCriteria criteria = new RefBookStatusCriteria();
        criteria.setNonArchived(true);

        final Page<UiRefBookStatus> page = controller.getStatusList(criteria);
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length - 1, page.getTotalElements());
        assertTrue(page.getContent().stream().noneMatch(status -> Objects.equals(RefBookStatus.ARCHIVED, status.getId())));
    }

    private RefBook createRefBook() {

        final RefBook result = new RefBook();
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REF_BOOK_CODE);
        result.setId(VERSION_ID);

        result.setDraftVersionId(DRAFT_ID);

        return  result;
    }

    private void assertRefBookEquals(RefBook expected, UiRefBook actual) {

        assertNotNull(actual);
        assertEquals(expected.getRefBookId(), actual.getRefBookId());
        assertEquals(expected.getCode(), actual.getCode());
        assertEquals(expected.getId(), actual.getId());
    }
}