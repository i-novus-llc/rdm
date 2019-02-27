package ru.inovus.ms.rdm.util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by znurgaliev on 13.08.2018.
 */
@RunWith(MockitoJUnitRunner.class)
public class SequenceVersionNumberStrategyTest {

    @InjectMocks
    SequenceVersionNumberStrategy versionNumberStrategy;

    @Mock
    RefBookVersionRepository versionRepository;

    @Test
    public void testNext() throws Exception {

        Structure structure1 = new Structure(new ArrayList<>(), new ArrayList<>());
        Structure.Attribute attribute = new Structure.Attribute();
        Structure structure2 = new Structure(Collections.singletonList(attribute), new ArrayList<>());

        RefBookVersionEntity e1 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "1.0", structure2);
        RefBookVersionEntity e2 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "20.5", structure1);
        RefBookVersionEntity e3 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "14.1", structure2);
        RefBookVersionEntity eqStructureDraft = createVersionEntity(RefBookVersionStatus.DRAFT, "0.0", structure1);
        RefBookVersionEntity notEqStructureDraft = createVersionEntity(RefBookVersionStatus.DRAFT, null, structure2);

        when(versionRepository.findAllByStatusAndRefBookId(eq(RefBookVersionStatus.PUBLISHED), eq(1)))
                .thenReturn(Arrays.asList(e1, e2, e3));

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(1)))
                .thenReturn(eqStructureDraft);
        Assert.assertEquals("20.6", versionNumberStrategy.next(1));

        when(versionRepository.findByStatusAndRefBookId(eq(RefBookVersionStatus.DRAFT), eq(1)))
                .thenReturn(notEqStructureDraft);
        Assert.assertEquals("21.0", versionNumberStrategy.next(1));

    }

    @Test
    public void testCheck() throws Exception {
        RefBookVersionEntity e1 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "1.0", null);
        RefBookVersionEntity e2 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "20.5", null);
        RefBookVersionEntity e3 = createVersionEntity(RefBookVersionStatus.PUBLISHED, "14.1", null);
        when(versionRepository.findAllByStatusAndRefBookId(eq(RefBookVersionStatus.PUBLISHED), eq(1)))
                .thenReturn(Arrays.asList(e1, e2, e3));

        Assert.assertFalse(versionNumberStrategy.check("1.0", 1));
        Assert.assertTrue(versionNumberStrategy.check("2.0", 1));
    }

    private RefBookVersionEntity createVersionEntity(RefBookVersionStatus status, String version, Structure structure){
        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setStatus(status);
        entity.setVersion(version);
        entity.setStructure(structure);
        return entity;
    }
}