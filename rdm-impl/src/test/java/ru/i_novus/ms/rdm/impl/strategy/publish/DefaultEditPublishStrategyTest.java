package ru.i_novus.ms.rdm.impl.strategy.publish;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.draft.PublishResponse;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEditPublishStrategyTest {

    @InjectMocks
    private DefaultEditPublishStrategy strategy;

    @Test
    public void testPublish() {

        RefBookVersionEntity editEntity = new RefBookVersionEntity();

        PublishResponse result = strategy.publish(editEntity);
        assertNotNull(result);
        assertNull(result.getRefBookCode());
        assertNull(result.getNewId());
    }
}