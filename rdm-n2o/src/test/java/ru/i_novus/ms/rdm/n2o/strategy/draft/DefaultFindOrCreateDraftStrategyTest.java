package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;
import ru.i_novus.ms.rdm.rest.client.impl.DraftRestServiceRestClient;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFindOrCreateDraftStrategyTest {

    private static final int REFBOOK_ID = 1;
    private static final String REFBOOK_CODE = "TEST_CODE";

    private static final int VERSION_ID = 2;
    private static final int DRAFT_ID = 3;

    @InjectMocks
    private DefaultFindOrCreateDraftStrategy strategy;

    @Mock
    private DraftRestServiceRestClient draftService;

    @Test
    public void testFindOrCreateWithVersion() {

        final RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.DRAFT);

        final UiDraft expected = new UiDraft(version);

        final UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verifyNoMoreInteractions(draftService);
    }

    @Test
    public void testFindOrCreateWithExistingDraft() {

        final RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);

        final Draft draft = createDraft();
        when(draftService.findDraft(eq(REFBOOK_CODE))).thenReturn(draft);

        final UiDraft expected = new UiDraft(draft, REFBOOK_ID);

        final UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verify(draftService).findDraft(eq(REFBOOK_CODE));

        verifyNoMoreInteractions(draftService);
    }

    @Test
    public void testFindOrCreateWithDraftCreate() {

        final RefBookVersion version = createRefBookVersion();
        version.setStatus(RefBookVersionStatus.PUBLISHED);

        when(draftService.findDraft(eq(REFBOOK_CODE))).thenReturn(null);

        final Draft draft = createDraft();
        when(draftService.createFromVersion(eq(VERSION_ID))).thenReturn(draft);

        final UiDraft expected = new UiDraft(draft, REFBOOK_ID);

        final UiDraft actual = strategy.findOrCreate(version);
        assertEquals(expected, actual);

        verify(draftService).findDraft(eq(REFBOOK_CODE));
        verify(draftService).createFromVersion(eq(VERSION_ID));

        verifyNoMoreInteractions(draftService);
    }

    private RefBookVersion createRefBookVersion() {

        final RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setRefBookId(REFBOOK_ID);
        result.setCode(REFBOOK_CODE);

        return result;
    }

    private Draft createDraft() {

        final Draft draft = new Draft();
        draft.setId(DRAFT_ID);

        return draft;
    }
}