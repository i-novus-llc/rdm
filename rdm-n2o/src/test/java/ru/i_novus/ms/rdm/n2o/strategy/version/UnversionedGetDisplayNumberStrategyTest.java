package ru.i_novus.ms.rdm.n2o.strategy.version;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedGetDisplayNumberStrategyTest {

    @InjectMocks
    private UnversionedGetDisplayNumberStrategy strategy;

    @Mock
    private Messages messages;

    @Test
    public void testGet() {

        final String versionNumber = "-1.0";

        RefBook refBook = new RefBook();
        refBook.setType(RefBookTypeEnum.UNVERSIONED);
        refBook.setStatus(RefBookVersionStatus.PUBLISHED);
        refBook.setVersion(versionNumber);

        when(messages.getMessage(any(String.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        String actual = strategy.get(refBook);
        assertEquals("refbook.display.number.unversioned", actual);

        verify(messages).getMessage(any(String.class));
        verifyNoMoreInteractions(messages);
    }
}