package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedEditPublishStrategyTest {

    @InjectMocks
    private UnversionedEditPublishStrategy strategy;

    @Mock
    private BasePublishStrategy basePublishStrategy;

    @Test
    public void testPublish() {

        RefBookVersionEntity editEntity = new RefBookVersionEntity();

        PublishResponse expected = new PublishResponse();
        expected.setRefBookCode("refbook_code");
        when(basePublishStrategy.publish(eq(editEntity), any(PublishRequest.class))).thenReturn(expected);

        PublishResponse actual = strategy.publish(editEntity);
        assertEquals(expected, actual);

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(basePublishStrategy).publish(eq(editEntity), requestCaptor.capture());

        PublishRequest request = requestCaptor.getValue();
        assertEquals(editEntity.getOptLockValue(), request.getOptLockValue());
        assertFalse(request.getResolveConflicts());

        verifyNoMoreInteractions(basePublishStrategy);
    }
}