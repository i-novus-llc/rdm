package ru.i_novus.ms.rdm.n2o.util;

import net.n2oapp.platform.i18n.Messages;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookOperation;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.n2o.model.UiRefBook;
import ru.i_novus.ms.rdm.n2o.strategy.BaseUiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategy;
import ru.i_novus.ms.rdm.n2o.strategy.UiStrategyLocator;
import ru.i_novus.ms.rdm.n2o.strategy.version.GetDisplayNumberStrategy;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@RunWith(MockitoJUnitRunner.class)
public class RefBookAdapterTest {

    @InjectMocks
    private RefBookAdapter adapter;

    @Mock
    private GetDisplayNumberStrategy getDisplayNumberStrategy;

    @Mock
    private Messages messages;

    @Before
    public void setUp() {

        final UiStrategyLocator strategyLocator = new BaseUiStrategyLocator(getStrategies());
        setField(adapter, "strategyLocator", strategyLocator);

        when(messages.getMessage(any(String.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);
    }

    @Test
    public void testToUiRefBook() {

        final RefBook refBook = new RefBook();

        final String displayNumber = "1.2";
        when(getDisplayNumberStrategy.get(eq(refBook))).thenReturn(displayNumber);

        final UiRefBook actual = adapter.toUiRefBook(refBook);
        assertEquals(displayNumber, actual.getDisplayNumber());
        assertNull(actual.getDisplayStatus());
        assertNull(actual.getDisplayOperation());
    }

    @Test
    public void testToUiRefBookWhenArchived() {

        final RefBook refBook = new RefBook();
        refBook.setArchived(Boolean.TRUE);

        final UiRefBook actual = adapter.toUiRefBook(refBook);
        assertEquals("refbook.display.status.archived", actual.getDisplayStatus());
    }

    @Test
    public void testToUiRefBookWhenOperation() {

        testToUiRefBookWhenOperation(RefBookOperation.PUBLISHING);
        testToUiRefBookWhenOperation(RefBookOperation.UPDATING);

        verify(messages, times(2)).getMessage(any(String.class));
        verifyNoMoreInteractions(messages);
    }

    private void testToUiRefBookWhenOperation(RefBookOperation operation) {

        final RefBook refBook = new RefBook();
        refBook.setCurrentOperation(operation);

        final String expectedDisplayOperation = "refbook.operation." + operation.name().toLowerCase();

        final UiRefBook actual = adapter.toUiRefBook(refBook);
        assertEquals(expectedDisplayOperation, actual.getDisplayOperation());
    }

    private Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> getStrategies() {

        final Map<RefBookTypeEnum, Map<Class<? extends UiStrategy>, UiStrategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends UiStrategy>, UiStrategy> getDefaultStrategies() {

        final Map<Class<? extends UiStrategy>, UiStrategy> result = new HashMap<>();
        // Version + Draft:
        result.put(GetDisplayNumberStrategy.class, getDisplayNumberStrategy);

        return result;
    }
}