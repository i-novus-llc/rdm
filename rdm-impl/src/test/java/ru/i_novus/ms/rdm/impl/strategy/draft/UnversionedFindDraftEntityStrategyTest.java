package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedFindDraftEntityStrategyTest {

    private static final int REFBOOK_ID = 1;

    @InjectMocks
    private UnversionedFindDraftEntityStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testFind() {

        RefBookEntity refBookEntity = createRefBookEntity();
        RefBookVersionEntity expected = new RefBookVersionEntity();
        expected.setRefBook(refBookEntity);
        expected.setStatus(RefBookVersionStatus.PUBLISHED);

        when(versionRepository.findFirstByRefBookIdAndStatusOrderByFromDateDesc(REFBOOK_ID, RefBookVersionStatus.PUBLISHED)).thenReturn(expected);

        RefBookVersionEntity actual = strategy.find(refBookEntity);
        assertEquals(expected, actual);
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity refBookEntity = new RefBookEntity();
        refBookEntity.setId(REFBOOK_ID);

        return refBookEntity;
    }
}