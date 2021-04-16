package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookType;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateRefBookEntityStrategyTest {

    @InjectMocks
    private DefaultCreateRefBookEntityStrategy strategy;

    @Test
    public void testCreate() {

        RefBookCreateRequest request = new RefBookCreateRequest("test_code", RefBookType.UNVERSIONED, "category", null);
        RefBookEntity entity = strategy.create(request);

        assertEquals(request.getCode(), entity.getCode());
        assertEquals(request.getType(), entity.getType());
        assertEquals(request.getCategory(), entity.getCategory());
    }
}