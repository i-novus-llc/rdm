package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedCreateFirstStorageStrategyTest {

    @InjectMocks
    private UnversionedCreateFirstStorageStrategy strategy;

    @Mock
    private DraftDataService draftDataService;

    @Test
    public void testCreate() {

        final String draftCode = "draft_code";
        when(draftDataService.createDraft(eq(emptyList()))).thenReturn(draftCode);
        final String appliedCode = "applied_code";
        when(draftDataService.applyDraftItself(eq(draftCode), any(LocalDateTime.class))).thenReturn(appliedCode);

        String result = strategy.create();
        assertEquals(appliedCode, result);

        verify(draftDataService).createDraft(eq(emptyList()));
        verify(draftDataService).applyDraftItself(eq(draftCode), any(LocalDateTime.class));
        verifyNoMoreInteractions(draftDataService);
    }
}