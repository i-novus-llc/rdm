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
import ru.i_novus.ms.rdm.n2o.model.*;
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

        RefBook refBook = createRefBook();

        List<RefBook> refBooks = singletonList(refBook);

        RefBookCriteria criteria = new RefBookCriteria();
        when(refBookService.search(eq(criteria)))
                .thenReturn(new PageImpl<>(refBooks, criteria, refBooks.size()));

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        Page<UiRefBook> page = controller.getList(criteria);
        assertNotNull(page.getContent());
        assertEquals(refBooks.size(), page.getTotalElements());

        UiRefBook actual = page.getContent().get(0);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testGetVersionRefBook() {

        RefBook refBook = createRefBook();
        when(refBookService.getByVersionId(VERSION_ID)).thenReturn(refBook);

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setVersionId(VERSION_ID);

        UiRefBook actual = controller.getVersionRefBook(criteria);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testGetVersionRefBookWhenExcludeDraft() {

        RefBook refBook = createRefBook();

        when(refBookService.getByVersionId(VERSION_ID)).thenReturn(refBook);

        RefBookCriteria criteria = new RefBookCriteria();
        criteria.setVersionId(VERSION_ID);

        when(rdmPermission.excludeDraft()).thenReturn(true);

        RefBook changed = new RefBook(refBook);
        changed.setDraftVersionId(null);
        when(refBookAdapter.toUiRefBook(eq(changed))).thenReturn(new UiRefBook(changed));

        UiRefBook actual = controller.getVersionRefBook(criteria);
        assertRefBookEquals(refBook, actual);

        assertNull(actual.getDraftVersionId());
        assertNull(changed.getDraftVersionId());
    }

    @Test
    public void testGetLastVersion() {

        RefBook refBook = createRefBook();

        RefBook oldBook = createRefBook();
        oldBook.setId(VERSION_ID + 1);

        List<RefBook> refBooks = List.of(refBook, oldBook);

        RefBookCriteria criteria = new RefBookCriteria();
        when(refBookService.searchVersions(eq(criteria)))
                .thenReturn(new PageImpl<>(refBooks, criteria, refBooks.size()));

        when(refBookAdapter.toUiRefBook(refBook)).thenReturn(new UiRefBook(refBook));

        UiRefBook actual = controller.getLastVersion(criteria);
        assertRefBookEquals(refBook, actual);
    }

    @Test
    public void testSearchReferenceRefBooks() {

        List<RefBook> refBooks = new ArrayList<>(1);

        RefBook refBook = createRefBook();
        refBooks.add(refBook);

        ArgumentCaptor<RefBookCriteria> captor = ArgumentCaptor.forClass(RefBookCriteria.class);
        when(refBookService.search(captor.capture())).thenAnswer(v ->
                new PageImpl<>(refBooks, (RefBookCriteria) v.getArguments()[0], 1)
        );

        RefBookCriteria criteria = new RefBookCriteria();
        Page<RefBook> page = controller.searchReferenceRefBooks(criteria);
        assertNotNull(page.getContent());
        assertEquals(refBooks.size(), page.getTotalElements());

        RefBookCriteria captured = captor.getValue();
        assertTrue(captured.getHasPublished());
        assertTrue(captured.getExcludeDraft());
        assertEquals(RefBookSourceType.LAST_PUBLISHED, captured.getSourceType());
    }

    @Test
    public void testGetTypeList() {

        Page<UiRefBookType> page = controller.getTypeList();
        assertNotNull(page.getContent());
        assertEquals(RefBookTypeEnum.values().length, page.getTotalElements());
    }

    @Test
    public void testGetTypeItem() {

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookTypeCriteria criteria = new RefBookTypeCriteria();
        criteria.setId(REFBOOK_TYPE_ID);

        UiRefBookType item = controller.getTypeItem(criteria);
        assertNotNull(item);
        assertEquals(criteria.getId(), item.getId());
        assertEquals(REFBOOK_TYPE_NAME, item.getName());
    }

    @Test
    public void testGetTypeItemByName() {

        when(messages.getMessage(any(String.class))).thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookTypeCriteria criteria = new RefBookTypeCriteria();
        criteria.setName(REFBOOK_TYPE_NAME);

        UiRefBookType item = controller.getTypeItem(criteria);
        assertNotNull(item);
        assertEquals(REFBOOK_TYPE_ID, item.getId());
        assertEquals(criteria.getName(), item.getName());
    }

    @Test
    public void testGetStatusList() {

        Page<UiRefBookStatus> page = controller.getStatusList(new RefBookStatusCriteria());
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length, page.getTotalElements());
    }

    @Test
    public void testGetStatusListWithoutDraft() {

        RefBookStatusCriteria criteria = new RefBookStatusCriteria();
        criteria.setExcludeDraft(true);

        Page<UiRefBookStatus> page = controller.getStatusList(criteria);
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length - 1, page.getTotalElements());
        assertTrue(page.getContent().stream().noneMatch(status -> Objects.equals(RefBookStatus.HAS_DRAFT, status.getId())));
    }

    @Test
    public void testGetStatusListWithoutArchived() {

        RefBookStatusCriteria criteria = new RefBookStatusCriteria();
        criteria.setNonArchived(true);

        Page<UiRefBookStatus> page = controller.getStatusList(criteria);
        assertNotNull(page.getContent());
        assertEquals(RefBookStatus.values().length - 1, page.getTotalElements());
        assertTrue(page.getContent().stream().noneMatch(status -> Objects.equals(RefBookStatus.ARCHIVED, status.getId())));
    }

    private RefBook createRefBook() {

        RefBook result = new RefBook();
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