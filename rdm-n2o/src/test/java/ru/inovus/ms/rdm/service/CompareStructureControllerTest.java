package ru.inovus.ms.rdm.service;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import ru.i_novus.platform.datastorage.temporal.enums.DiffStatusEnum;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.AttributeDiff;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.StructureDiff;
import ru.inovus.ms.rdm.model.CompareCriteria;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareStructureControllerTest {

    @InjectMocks
    private CompareStructureController compareStructureController;

    @Mock
    private VersionService versionService;
    @Mock
    private CompareService compareService;

    private final static Integer oldId = 1;
    private final static Integer newId = 2;


    @Before
    public void init() {
        when(versionService.getStructure(oldId)).thenReturn(new Structure(Arrays.asList(
                Structure.Attribute.build("code1", "Код1", FieldType.STRING, false, "Описание1"),
                Structure.Attribute.build("code2", "Код2", FieldType.STRING, false, "Описание2"),
                Structure.Attribute.build("code3", "Код3", FieldType.STRING, false, "Описание3"),
                Structure.Attribute.build("code4", "Код4", FieldType.STRING, false, "Описание4")
        ), null));
        when(versionService.getStructure(newId)).thenReturn(new Structure(Arrays.asList(
                Structure.Attribute.build("code1", "Код1", FieldType.STRING, false, "Описание1"),
                Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, false, "Описание2.1"),
                Structure.Attribute.build("code5", "Код5", FieldType.STRING, false, "Описание5"),
                Structure.Attribute.build("code4", "Код4", FieldType.STRING, false, "Описание4")
        ), null));
        when(compareService.compareStructures(oldId, newId)).thenReturn(new StructureDiff(
                Arrays.asList(
                        new StructureDiff.AttributeDiff(
                                null,
                                Structure.Attribute.build("code5", "Код5", FieldType.STRING, false, "Описание5"))
                ),
                Arrays.asList(
                        new StructureDiff.AttributeDiff(
                                Structure.Attribute.build("code2", "Код2", FieldType.STRING, false, "Описание2"),
                                Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, false, "Описание2.1"))
                ),
                Arrays.asList(
                        new StructureDiff.AttributeDiff(
                                Structure.Attribute.build("code3", "Код3", FieldType.STRING, false, "Описание3"),
                                null)
                )
        ));
    }

    @Test
    public void testGetCommonDiff() throws Exception {
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, false, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, false, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, false, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, false, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, false, "Описание4"), null),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, false, "Описание3"), null, DiffStatusEnum.DELETED)
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
    public void testGetOldWithDiff() throws Exception {
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(Structure.Attribute.build("code1", "Код1", FieldType.STRING, false, "Описание1"), null, null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, false, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, false, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(Structure.Attribute.build("code3", "Код3", FieldType.STRING, false, "Описание3"), null, DiffStatusEnum.DELETED),
                createDiff(Structure.Attribute.build("code4", "Код4", FieldType.STRING, false, "Описание4"), null, null)
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
    public void testGetNewWithDiff() throws Exception {
        List<AttributeDiff> expectedCommon = Arrays.asList(
                createDiff(null, Structure.Attribute.build("code1", "Код1", FieldType.STRING, false, "Описание1"), null),
                createDiff(Structure.Attribute.build("code2", "Код2", FieldType.STRING, false, "Описание2"),
                        Structure.Attribute.build("code2", "Код2.1", FieldType.STRING, false, "Описание2.1"), DiffStatusEnum.UPDATED),
                createDiff(null, Structure.Attribute.build("code5", "Код5", FieldType.STRING, false, "Описание5"), DiffStatusEnum.INSERTED),
                createDiff(null, Structure.Attribute.build("code4", "Код4", FieldType.STRING, false, "Описание4"), null)
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
        return new AttributeDiff(
                newAttr.getCode() != null ? newAttr.getCode() : oldAttr.getCode(),
                new AttributeDiff.AttributeFieldDiff(oldAttr.getName(), newAttr.getName()),
                new AttributeDiff.AttributeFieldDiff(oldAttr.getType(), newAttr.getType()),
                new AttributeDiff.AttributeFieldDiff(oldAttr.getIsPrimary(), newAttr.getIsPrimary()),
                new AttributeDiff.AttributeFieldDiff(oldAttr.getIsRequired(), newAttr.getIsRequired()),
                new AttributeDiff.AttributeFieldDiff(oldAttr.getDescription(), newAttr.getDescription()),
                diffStatus
        );
    }
}