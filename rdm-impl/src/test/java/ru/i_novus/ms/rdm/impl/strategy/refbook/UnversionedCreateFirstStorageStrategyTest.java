package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.service.StorageService;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedCreateFirstStorageStrategyTest {

    @InjectMocks
    private UnversionedCreateFirstStorageStrategy strategy;

    @Mock
    private StorageService storageService;

    @Test
    public void testCreate() {

        final String draftCode = "draft_code";
        when(storageService.createStorage(eq(emptyList()))).thenReturn(draftCode);

        String result = strategy.create();
        assertEquals(draftCode, result);

        verify(storageService).createStorage(eq(emptyList()));
        verifyNoMoreInteractions(storageService);
    }
}