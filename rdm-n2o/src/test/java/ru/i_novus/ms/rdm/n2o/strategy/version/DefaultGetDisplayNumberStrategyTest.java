package ru.i_novus.ms.rdm.n2o.strategy.version;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultGetDisplayNumberStrategyTest {

    @InjectMocks
    private DefaultGetDisplayNumberStrategy strategy;

    @Mock
    private Messages messages;

    @Test
    public void testGetWhenPublished() {

        final String versionNumber = "1.2";

        RefBook refBook = new RefBook();
        refBook.setStatus(RefBookVersionStatus.PUBLISHED);
        refBook.setVersion(versionNumber);

        String actual = strategy.get(refBook);
        assertEquals(versionNumber, actual);

        verifyNoMoreInteractions(messages);
    }

    @Test
    public void testGetWhenDraft() {

        final String versionNumber = "0.0";
        final String editDate = "01.02.2003";

        RefBook refBook = new RefBook();
        refBook.setStatus(RefBookVersionStatus.DRAFT);
        refBook.setVersion(versionNumber);
        refBook.setEditDate(LocalDateTime.of(LocalDate.of(2003, 2, 1), LocalTime.MIDNIGHT));

        when(messages.getMessage(any(String.class), eq(editDate)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        String actual = strategy.get(refBook);
        assertEquals("refbook.display.number.draft", actual);

        verify(messages).getMessage(any(String.class), eq(editDate));
        verifyNoMoreInteractions(messages);
    }
}