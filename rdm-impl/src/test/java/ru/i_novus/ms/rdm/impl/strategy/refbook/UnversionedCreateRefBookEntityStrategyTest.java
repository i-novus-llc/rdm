package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCreateRequest;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.UnversionedRefBookEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedCreateRefBookEntityStrategyTest {

    @InjectMocks
    private UnversionedCreateRefBookEntityStrategy strategy;

    @Mock
    private RefBookRepository refBookRepository;

    @Test
    public void testCreate() {

        when(refBookRepository.save(any(UnversionedRefBookEntity.class)))
                .thenAnswer(invocation -> invocation.getArguments()[0]);

        RefBookCreateRequest request = new RefBookCreateRequest("test_code", RefBookTypeEnum.UNVERSIONED, "category", null);
        RefBookEntity entity = strategy.create(request);

        assertNotNull(entity);
        assertEqualsRequestToEntity(request, entity);

        verify(refBookRepository).save(any(UnversionedRefBookEntity.class));
        verifyNoMoreInteractions(refBookRepository);
    }

    private void assertEqualsRequestToEntity(RefBookCreateRequest request, RefBookEntity entity) {

        assertEquals(request.getCode(), entity.getCode());
        assertEquals(request.getType(), entity.getType());
        assertEquals(request.getCategory(), entity.getCategory());
    }
}