package ru.i_novus.ms.rdm.impl.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus.DRAFT;
import static ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus.PUBLISHED;

/**
 * Created by znurgaliev on 13.08.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class SequenceVersionNumberStrategyTest {

    @InjectMocks
    private SequenceVersionNumberStrategy strategy;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Test
    public void testFirst() {

        String version = strategy.first();
        assertNotNull(version);
    }

    @Test
    public void testNext() {

        Structure structure1 = new Structure(new ArrayList<>(), new ArrayList<>());
        Structure.Attribute attribute = new Structure.Attribute();
        Structure structure2 = new Structure(Collections.singletonList(attribute), new ArrayList<>());

        RefBookVersionEntity e1 = createVersionEntity(PUBLISHED, "1.0", structure2);
        RefBookVersionEntity e2 = createVersionEntity(PUBLISHED, "20.5", structure1);
        RefBookVersionEntity e3 = createVersionEntity(PUBLISHED, "14.1", structure2);
        RefBookVersionEntity eqStructureDraft = createVersionEntity(DRAFT, "0.0", structure1);
        RefBookVersionEntity notEqStructureDraft = createVersionEntity(DRAFT, null, structure2);

        when(versionRepository.findAllByStatusAndRefBookId(eq(PUBLISHED), eq(1)))
                .thenReturn(Arrays.asList(e1, e2, e3));

        when(versionRepository.findByStatusAndRefBookId(eq(DRAFT), eq(1)))
                .thenReturn(eqStructureDraft);
        Assert.assertEquals("20.6", strategy.next(1));

        when(versionRepository.findByStatusAndRefBookId(eq(DRAFT), eq(1)))
                .thenReturn(notEqStructureDraft);
        Assert.assertEquals("21.0", strategy.next(1));

    }

    @Test
    public void testCheck() {

        RefBookVersionEntity e1 = createVersionEntity(PUBLISHED, "1.0", null);
        RefBookVersionEntity e2 = createVersionEntity(PUBLISHED, "20.5", null);
        RefBookVersionEntity e3 = createVersionEntity(PUBLISHED, "14.1", null);
        when(versionRepository.findAllByStatusAndRefBookId(eq(PUBLISHED), eq(1)))
                .thenReturn(Arrays.asList(e1, e2, e3));

        Assert.assertFalse(strategy.check("1.0", 1));
        Assert.assertTrue(strategy.check("2.0", 1));
    }

    private RefBookVersionEntity createVersionEntity(RefBookVersionStatus status, String version, Structure structure){

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setStatus(status);
        entity.setVersion(version);
        entity.setStructure(structure);

        return entity;
    }
}