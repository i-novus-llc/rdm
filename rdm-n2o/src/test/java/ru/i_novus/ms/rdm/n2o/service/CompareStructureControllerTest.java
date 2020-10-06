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
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.n2o.model.AttributeDiff;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareStructureControllerTest {

    @InjectMocks
    private CompareStructureController compareStructureController;

    @Mock
    private VersionRestService versionService;
    @Mock
    private CompareService compareService;

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
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"), null, DiffStatusEnum.DELETED)
        );
        List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedSecondPage = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(Collectors.toList());

        Page<AttributeDiff> actual = compareStructureController.getCommonDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(5, actual.getTotalElements());
        Assert.assertEquals(expectedCommon, actual.getContent());

        actual = compareStructureController.getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedInserted, actual.getContent());

        actual = compareStructureController.getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedUpdated, actual.getContent());

        actual = compareStructureController.getCommonDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedDeleted, actual.getContent());

        CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);
        actual = compareStructureController.getCommonDiff(criteria);
        Assert.assertEquals(5, actual.getTotalElements());
        Assert.assertEquals(expectedSecondPage, actual.getContent());

    }

    @Test
    public void testGetOldWithDiff() {
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null, null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, "Описание3"), null, DiffStatusEnum.DELETED),
                createDiff(Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null, null)
        );
        List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedSecondPage = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(Collectors.toList());

        Page<AttributeDiff> actual = compareStructureController.getOldWithDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(4, actual.getTotalElements());
        Assert.assertEquals(expectedCommon, actual.getContent());

        actual = compareStructureController.getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(0, actual.getTotalElements());
        Assert.assertEquals(expectedInserted, actual.getContent());

        actual = compareStructureController.getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedUpdated, actual.getContent());

        actual = compareStructureController.getOldWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedDeleted, actual.getContent());

        CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);
        actual = compareStructureController.getOldWithDiff(criteria);
        Assert.assertEquals(4, actual.getTotalElements());
        Assert.assertEquals(expectedSecondPage, actual.getContent());

    }

    @Test
    public void testGetNewWithDiff() {
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, "Описание4"), null)
        );
        List<AttributeDiff> expectedInserted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.INSERTED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedUpdated = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.UPDATED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedDeleted = expectedCommon.stream()
                .filter(attributeDiff -> DiffStatusEnum.DELETED.equals(attributeDiff.getDiffStatus()))
                .collect(Collectors.toList());
        List<AttributeDiff> expectedSecondPage = expectedCommon.stream()
                .skip(2)
                .limit(2)
                .collect(Collectors.toList());

        Page<AttributeDiff> actual = compareStructureController.getNewWithDiff(new CompareCriteria(oldId, newId, null));
        Assert.assertEquals(4, actual.getTotalElements());
        Assert.assertEquals(expectedCommon, actual.getContent());

        actual = compareStructureController.getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.INSERTED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedInserted, actual.getContent());

        actual = compareStructureController.getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.UPDATED));
        Assert.assertEquals(1, actual.getTotalElements());
        Assert.assertEquals(expectedUpdated, actual.getContent());

        actual = compareStructureController.getNewWithDiff(new CompareCriteria(oldId, newId, DiffStatusEnum.DELETED));
        Assert.assertEquals(0, actual.getTotalElements());
        Assert.assertEquals(expectedDeleted, actual.getContent());

        CompareCriteria criteria = new CompareCriteria(oldId, newId, null);
        criteria.setPageNumber(1);
        criteria.setPageSize(2);
        actual = compareStructureController.getNewWithDiff(criteria);
        Assert.assertEquals(4, actual.getTotalElements());
        Assert.assertEquals(expectedSecondPage, actual.getContent());
    }

    private AttributeDiff createDiff(Structure.Attribute oldAttr, Structure.Attribute newAttr, DiffStatusEnum diffStatus) {

        oldAttr = oldAttr != null ? oldAttr : new Structure.Attribute();
        newAttr = newAttr != null ? newAttr : new Structure.Attribute();

        return new AttributeDiff(oldAttr, newAttr, diffStatus);
    }
}