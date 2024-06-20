package ru.i_novus.ms.rdm.n2o.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.CompareCriteria;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.n2o.model.AttributeDiff;
import ru.i_novus.ms.rdm.rest.client.impl.CompareServiceRestClient;
import ru.i_novus.ms.rdm.rest.client.impl.VersionRestServiceRestClient;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareStructureControllerTest {

    @InjectMocks
    private CompareStructureController compareStructureController;

    @Mock
    private VersionRestServiceRestClient versionService;
    @Mock
    private CompareServiceRestClient compareService;

    private final static Integer oldId = 1;
    private final static Integer newId = 2;


    @Before
    public void init() {

        when(versionService.getStructure(oldId)).thenReturn(new Structure(Arrays.asList(
                Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"),
                Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"),
                Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4")
        ), null));

        when(versionService.getStructure(newId)).thenReturn(new Structure(Arrays.asList(
                Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"),
                Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"),
                Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"),
                Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4")
        ), null));

        when(compareService.compareStructures(oldId, newId)).thenReturn(new StructureDiff(
                singletonList(
                        new StructureDiff.AttributeDiff(
                                null,
                                Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"))
                ),
                singletonList(
                        new StructureDiff.AttributeDiff(
                                Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                                Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"))
                ),
                singletonList(
                        new StructureDiff.AttributeDiff(
                                Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"),
                                null)
                )
        ));
    }

    @Test
    public void testGetCommonDiff() {

        final List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"), null, DiffStatusEnum.DELETED)
        );

        final List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedPaged = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(toList());

        final Page<AttributeDiff> actualCommon = compareStructureController
                .getCommonDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(5, actualCommon.getTotalElements());
        Assert.assertEquals(expectedCommon, actualCommon.getContent());

        final Page<AttributeDiff> actualInserted = compareStructureController
                .getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(1, actualInserted.getTotalElements());
        Assert.assertEquals(expectedInserted, actualInserted.getContent());

        final Page<AttributeDiff> actualUpdated = compareStructureController
                .getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actualUpdated.getTotalElements());
        Assert.assertEquals(expectedUpdated, actualUpdated.getContent());

        final Page<AttributeDiff> actualDeleted = compareStructureController
                .getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(1, actualDeleted.getTotalElements());
        Assert.assertEquals(expectedDeleted, actualDeleted.getContent());

        final CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);

        final Page<AttributeDiff> actualPaged = compareStructureController.getCommonDiff(criteria);
        Assert.assertEquals(5, actualPaged.getTotalElements());
        Assert.assertEquals(expectedPaged, actualPaged.getContent());
    }

    @Test
    public void testGetOldWithDiff() {

        final List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null, null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"), null, DiffStatusEnum.DELETED),
                createDiff(Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null, null)
        );

        final List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedPaged = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(toList());

        final Page<AttributeDiff> actualCommon = compareStructureController
                .getOldWithDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(4, actualCommon.getTotalElements());
        Assert.assertEquals(expectedCommon, actualCommon.getContent());

        final Page<AttributeDiff> actualInserted = compareStructureController
                .getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(0, actualInserted.getTotalElements());
        Assert.assertEquals(expectedInserted, actualInserted.getContent());

        final Page<AttributeDiff> actualUpdated = compareStructureController
                .getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actualUpdated.getTotalElements());
        Assert.assertEquals(expectedUpdated, actualUpdated.getContent());

        final Page<AttributeDiff> actualDeleted = compareStructureController
                .getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(1, actualDeleted.getTotalElements());
        Assert.assertEquals(expectedDeleted, actualDeleted.getContent());

        final CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);

        final Page<AttributeDiff> actualPaged = compareStructureController.getOldWithDiff(criteria);
        Assert.assertEquals(4, actualPaged.getTotalElements());
        Assert.assertEquals(expectedPaged, actualPaged.getContent());

    }

    @Test
    public void testGetNewWithDiff() {

        final List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null)
        );

        final List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(toList());

        final List<AttributeDiff> expectedPaged = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(toList());

        final Page<AttributeDiff> actualCommon = compareStructureController
                .getNewWithDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(4, actualCommon.getTotalElements());
        Assert.assertEquals(expectedCommon, actualCommon.getContent());

        final Page<AttributeDiff> actualInserted = compareStructureController
                .getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(1, actualInserted.getTotalElements());
        Assert.assertEquals(expectedInserted, actualInserted.getContent());

        final Page<AttributeDiff> actualUpdated = compareStructureController
                .getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actualUpdated.getTotalElements());
        Assert.assertEquals(expectedUpdated, actualUpdated.getContent());

        final Page<AttributeDiff> actualDeleted = compareStructureController
                .getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(0, actualDeleted.getTotalElements());
        Assert.assertEquals(expectedDeleted, actualDeleted.getContent());

        final CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);

        final Page<AttributeDiff> actualPaged = compareStructureController.getNewWithDiff(criteria);
        Assert.assertEquals(4, actualPaged.getTotalElements());
        Assert.assertEquals(expectedPaged, actualPaged.getContent());
    }

    private AttributeDiff createDiff(Structure.Attribute oldAttr, Structure.Attribute newAttr, DiffStatusEnum diffStatus) {

        return new AttributeDiff(
                oldAttr != null ? oldAttr : new Structure.Attribute(),
                newAttr != null ? newAttr : new Structure.Attribute(),
                diffStatus
        );
    }
}