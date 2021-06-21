package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateRefBookEntityStrategyTest {

    @InjectMocks
    private DefaultCreateRefBookEntityStrategy strategy;

    @Mock
    private RefBookRepository refBookRepository;

    @Test
    public void testCreate() {

        when(refBookRepository.save(any(DefaultRefBookEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookCreateRequest request = new RefBookCreateRequest("test_code", null, "category", null);
        RefBookEntity entity = strategy.create(request);

        assertNotNull(entity);
        request.setType(RefBookTypeEnum.DEFAULT);
        assertEqualsRequestToEntity(request, entity);

        verify(refBookRepository).save(any(DefaultRefBookEntity.class));
        verifyNoMoreInteractions(refBookRepository);
    }

    private void assertEqualsRequestToEntity(RefBookCreateRequest request, RefBookEntity entity) {

        assertEquals(request.getCode(), entity.getCode());
        assertEquals(request.getType(), entity.getType());
        assertEquals(request.getCategory(), entity.getCategory());
    }
}