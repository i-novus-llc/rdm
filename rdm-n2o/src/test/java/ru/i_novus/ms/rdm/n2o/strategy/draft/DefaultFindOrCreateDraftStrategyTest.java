package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFindOrCreateDraftStrategyTest {

    private static final int REFBOOK_ID = 1;
    private static final String REFBOOK_CODE = "TEST_CODE";

    private static final int VERSION_ID = 2;
    private static final int DRAFT_ID = 2;

    @InjectMocks
    private DefaultFindOrCreateDraftStrategy strategy;

    @Mock
    private DraftRestService draftService;

    @Test
    public void testFindOrCreateWithVersion() {

        RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);

        UiDraft expected = new UiDraft(version);

        UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verifyNoMoreInteractions(draftService);
    }

    @Test
    public void testFindOrCreateWithExistingDraft() {

        RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);

        Draft draft = createDraft();

        when(draftService.findDraft(eq(REFBOOK_CODE))).thenReturn(draft);

        UiDraft expected = new UiDraft(draft, REFBOOK_ID);

        UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verify(draftService).findDraft(eq(REFBOOK_CODE));

        verifyNoMoreInteractions(draftService);
    }

    @Test
    public void testFindOrCreateWithDraftCreate() {

        RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);

        when(draftService.findDraft(eq(REFBOOK_CODE))).thenReturn(null);

        Draft draft = createDraft();

        when(draftService.createFromVersion(eq(VERSION_ID))).thenReturn(draft);

        UiDraft expected = new UiDraft(draft, REFBOOK_ID);

        UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verify(draftService).findDraft(eq(REFBOOK_CODE));
        verify(draftService).createFromVersion(eq(VERSION_ID));

        verifyNoMoreInteractions(draftService);
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);

        return result;
    }

    private Draft createDraft() {

        Draft draft = new Draft();
        draft.setId(DRAFT_ID);

        return draft;
    }
}