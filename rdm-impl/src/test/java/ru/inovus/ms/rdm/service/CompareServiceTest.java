package ru.inovus.ms.rdm.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.PassportAttributeDiff;
import ru.inovus.ms.rdm.model.PassportDiff;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompareServiceTest {

    private static final String PASSPORT_ATTRIBUTE_FULL_NAME = "TEST_fullName";
    private static final String PASSPORT_ATTRIBUTE_SHORT_NAME = "TEST_shortName";
    private static final String PASSPORT_ATTRIBUTE_ANNOTATION = "TEST_annotation";
    private static final String PASSPORT_ATTRIBUTE_GROUP = "TEST_group";
    private static final String PASSPORT_ATTRIBUTE_TYPE = "TEST_type";

    @InjectMocks
    private CompareServiceImpl compareService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private PassportAttributeRepository passportAttributeRepository;

    private static PassportAttributeEntity passportAttributeFullName;
    private static PassportAttributeEntity passportAttributeShortName;
    private static PassportAttributeEntity passportAttributeAnnotation;
    private static PassportAttributeEntity passportAttributeGroup;
    private static PassportAttributeEntity passportAttributeType;
    private static List<PassportAttributeEntity> passportAttributeEntities;

    private static RefBookVersionEntity leftVersion;
    private static RefBookVersionEntity rightVersion;

    @BeforeClass
    public static void initialize() {
        passportAttributeFullName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_FULL_NAME, "Полное название");
        passportAttributeShortName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_SHORT_NAME, "Краткое название");
        passportAttributeAnnotation= new PassportAttributeEntity(PASSPORT_ATTRIBUTE_ANNOTATION, "Аннотация");
        passportAttributeGroup = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_GROUP, "Группа");
        passportAttributeType = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_TYPE, "Тип");

        passportAttributeEntities = asList(passportAttributeFullName, passportAttributeShortName, passportAttributeAnnotation, passportAttributeGroup, passportAttributeType);
    }

    @Before
    public void setUp() {

        leftVersion = new RefBookVersionEntity();
        leftVersion.setId(2);
        leftVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        rightVersion = new RefBookVersionEntity();
        rightVersion.setId(3);
        rightVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        when(versionRepository.getOne(leftVersion.getId())).thenReturn(leftVersion);
        when(versionRepository.getOne(rightVersion.getId())).thenReturn(rightVersion);
        when(passportAttributeRepository.findAllByComparableIsTrue()).thenReturn(passportAttributeEntities);
    }

    @Test
    public void testComparePassports() {
        Set<PassportValueEntity> leftPassportValues = new HashSet<>();
        leftPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", leftVersion));
        leftPassportValues.add(new PassportValueEntity(passportAttributeShortName, "short_name", leftVersion));
        leftPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", leftVersion));
        leftPassportValues.add(new PassportValueEntity(passportAttributeType, null, leftVersion));

        leftVersion.setPassportValues(leftPassportValues);

        Set<PassportValueEntity> rightPassportValues = new HashSet<>();
        rightPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", rightVersion));
        rightPassportValues.add(new PassportValueEntity(passportAttributeAnnotation, "annotation", rightVersion));
        rightPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", rightVersion));
        rightPassportValues.add(new PassportValueEntity(passportAttributeType, null, rightVersion));

        rightVersion.setPassportValues(rightPassportValues);

        PassportDiff passportDiff = compareService.comparePassports(leftVersion.getId(), rightVersion.getId());
        validatePassportAttributes(passportDiff.getPassportAttributeDiffs(), asList(
                passportAttributeFullName.getCode(),
                passportAttributeShortName.getCode(),
                passportAttributeAnnotation.getCode()
        ));
        validatePassportAttributeDiffs(passportDiff.getPassportAttributeDiffs(), leftPassportValues, rightPassportValues);
    }

    @Test
    public void testComparePassportsWhenUpdateAttributeValue() {
        Set<PassportValueEntity> leftPassportValues = new HashSet<>();
        leftPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", leftVersion));

        leftVersion.setPassportValues(leftPassportValues);

        Set<PassportValueEntity> rightPassportValues = new HashSet<>();
        rightPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", rightVersion));

        rightVersion.setPassportValues(rightPassportValues);

        PassportDiff passportDiff = compareService.comparePassports(leftVersion.getId(), rightVersion.getId());
        validatePassportAttributes(passportDiff.getPassportAttributeDiffs(), singletonList(passportAttributeFullName.getCode()));
        validatePassportAttributeDiffs(passportDiff.getPassportAttributeDiffs(), leftPassportValues, rightPassportValues);
    }

    @Test
    public void testComparePassportsWhenAddAttributeValue() {
        Set<PassportValueEntity> leftPassportValues = new HashSet<>();

        leftVersion.setPassportValues(leftPassportValues);

        Set<PassportValueEntity> rightPassportValues = new HashSet<>();
        leftPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", leftVersion));

        rightVersion.setPassportValues(rightPassportValues);

        PassportDiff passportDiff = compareService.comparePassports(leftVersion.getId(), rightVersion.getId());
        validatePassportAttributes(passportDiff.getPassportAttributeDiffs(), singletonList(passportAttributeFullName.getCode()));
        validatePassportAttributeDiffs(passportDiff.getPassportAttributeDiffs(), leftPassportValues, rightPassportValues);
    }

    @Test
    public void testComparePassportsWhenDeleteAttributeValue() {
        Set<PassportValueEntity> leftPassportValues = new HashSet<>();
        leftPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", leftVersion));

        leftVersion.setPassportValues(leftPassportValues);

        Set<PassportValueEntity> rightPassportValues = new HashSet<>();

        rightVersion.setPassportValues(rightPassportValues);

        PassportDiff passportDiff = compareService.comparePassports(leftVersion.getId(), rightVersion.getId());
        validatePassportAttributes(passportDiff.getPassportAttributeDiffs(), singletonList(passportAttributeFullName.getCode()));
        validatePassportAttributeDiffs(passportDiff.getPassportAttributeDiffs(), leftPassportValues, rightPassportValues);
    }

    private void validatePassportAttributes(List<PassportAttributeDiff> passportAttributeDiffs, List<String> passportAttributeCodes) {
        assertEquals(passportAttributeCodes.size(), passportAttributeDiffs.size());
        passportAttributeCodes.forEach(passportAttributeCode -> {
            if (passportAttributeDiffs.stream().noneMatch(passportAttributeDiff -> passportAttributeCode.equals(passportAttributeDiff.getPassportAttribute().getCode())))
                Assert.fail("Attribute \"" + passportAttributeCode + "\" must be in diff");
        });
    }

    private void validatePassportAttributeDiffs(List<PassportAttributeDiff> passportAttributeDiffs, Set<PassportValueEntity> leftPassportValues, Set<PassportValueEntity> rightPassportValues) {
        passportAttributeDiffs.forEach(passportAttributeDiff -> {
            PassportValueEntity leftPassportValue = leftPassportValues.stream().filter(passportValueEntity -> passportValueEntity.getAttribute().getCode().equals(passportAttributeDiff.getPassportAttribute().getCode())).findFirst().orElse(null);
            PassportValueEntity rightPassportValue = rightPassportValues.stream().filter(passportValueEntity -> passportValueEntity.getAttribute().getCode().equals(passportAttributeDiff.getPassportAttribute().getCode())).findFirst().orElse(null);
            assertTrue(equalValues(leftPassportValue, passportAttributeDiff.getLeftValue()));
            assertTrue(equalValues(rightPassportValue, passportAttributeDiff.getRightValue()));
        });
    }

    private boolean equalValues(PassportValueEntity passportValue, String value) {
        if (passportValue != null && passportValue.getValue() != null)
            return value != null && passportValue.getValue().equals(value);
        else
            return value == null;
    }

}