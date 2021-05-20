package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.impl.entity.DefaultRefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFindDraftEntityStrategyTest {

    private static final int REFBOOK_ID = 1;

    @InjectMocks
    private DefaultFindDraftEntityStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testFind() {

        RefBookEntity refBookEntity = createRefBookEntity();
        RefBookVersionEntity expected = new RefBookVersionEntity();
        expected.setRefBook(refBookEntity);
        expected.setStatus(RefBookVersionStatus.DRAFT);

        when(versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, REFBOOK_ID)).thenReturn(expected);

        RefBookVersionEntity actual = strategy.find(refBookEntity);
        assertEquals(expected, actual);
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity refBookEntity = new DefaultRefBookEntity();
        refBookEntity.setId(REFBOOK_ID);

        return refBookEntity;
    }
}