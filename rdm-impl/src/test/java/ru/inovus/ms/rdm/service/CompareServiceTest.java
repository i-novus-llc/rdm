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
import ru.inovus.ms.rdm.model.PassportAttribute;
import ru.inovus.ms.rdm.model.PassportAttributeDiff;
import ru.inovus.ms.rdm.model.PassportDiff;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
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

    private static RefBookVersionEntity firstVersion;
    private static RefBookVersionEntity secondVersion;

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

        firstVersion = new RefBookVersionEntity();
        firstVersion.setId(2);
        firstVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        secondVersion = new RefBookVersionEntity();
        secondVersion.setId(3);
        secondVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        when(versionRepository.getOne(firstVersion.getId())).thenReturn(firstVersion);
        when(versionRepository.getOne(secondVersion.getId())).thenReturn(secondVersion);
        when(passportAttributeRepository.findAllByComparableIsTrue()).thenReturn(passportAttributeEntities);
    }

    @Test
    public void testComparePassports() {
        Set<PassportValueEntity> firstPassportValues = new HashSet<>();
        firstPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", firstVersion));
        firstPassportValues.add(new PassportValueEntity(passportAttributeShortName, "short_name", firstVersion));
        firstPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", firstVersion));
        firstPassportValues.add(new PassportValueEntity(passportAttributeType, null, firstVersion));
        firstVersion.setPassportValues(firstPassportValues);

        Set<PassportValueEntity> secondPassportValues = new HashSet<>();
        secondPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", secondVersion));
        secondPassportValues.add(new PassportValueEntity(passportAttributeAnnotation, "annotation", secondVersion));
        secondPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", secondVersion));
        secondPassportValues.add(new PassportValueEntity(passportAttributeType, null, secondVersion));
        secondVersion.setPassportValues(secondPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(firstVersion.getId(), secondVersion.getId());

        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                "full_name_upd"
                ));
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeShortName.getCode(), passportAttributeShortName.getName()),
                "short_name",
                null
                ));
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeAnnotation.getCode(), passportAttributeAnnotation.getName()),
                null,
                "annotation"
                ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenUpdateAttributeValue() {
        Set<PassportValueEntity> firstPassportValues = new HashSet<>();
        firstPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", firstVersion));
        firstVersion.setPassportValues(firstPassportValues);

        Set<PassportValueEntity> secondPassportValues = new HashSet<>();
        secondPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", secondVersion));
        secondVersion.setPassportValues(secondPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(firstVersion.getId(), secondVersion.getId());

        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                "full_name_upd"
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenAddAttributeValue() {
        Set<PassportValueEntity> firstPassportValues = new HashSet<>();
        firstVersion.setPassportValues(firstPassportValues);

        Set<PassportValueEntity> secondPassportValues = new HashSet<>();
        secondPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", secondVersion));
        secondVersion.setPassportValues(secondPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(firstVersion.getId(), secondVersion.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                null,
                "full_name"
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenDeleteAttributeValue() {
        Set<PassportValueEntity> firstPassportValues = new HashSet<>();
        firstPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", firstVersion));
        firstVersion.setPassportValues(firstPassportValues);

        Set<PassportValueEntity> secondPassportValues = new HashSet<>();
        secondVersion.setPassportValues(secondPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(firstVersion.getId(), secondVersion.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        expectedPassportAttributeDiffList.add(new PassportAttributeDiff(
                new PassportAttribute(passportAttributeFullName.getCode(), passportAttributeFullName.getName()),
                "full_name",
                null
        ));
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    @Test
    public void testComparePassportsWhenNoDiff() {
        Set<PassportValueEntity> firstPassportValues = new HashSet<>();
        firstPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", firstVersion));
        firstVersion.setPassportValues(firstPassportValues);

        Set<PassportValueEntity> secondPassportValues = new HashSet<>();
        secondPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", secondVersion));
        secondVersion.setPassportValues(secondPassportValues);

        PassportDiff actualPassportDiff = compareService.comparePassports(firstVersion.getId(), secondVersion.getId());
        List<PassportAttributeDiff> expectedPassportAttributeDiffList = new ArrayList<>();
        PassportDiff expectedPassportDiff = new PassportDiff(expectedPassportAttributeDiffList);

        assertPassportDiffs(expectedPassportDiff, actualPassportDiff);
    }

    private void assertPassportDiffs(PassportDiff expectedPassportDiff, PassportDiff actualPassportDiff) {
        assertEquals(expectedPassportDiff.getPassportAttributeDiffs().size(), actualPassportDiff.getPassportAttributeDiffs().size());
        expectedPassportDiff.getPassportAttributeDiffs().forEach(expectedPassportAttributeDiff -> {
            PassportAttributeDiff actualPassportAttributeDiff = actualPassportDiff.getPassportAttributeDiffs().stream().filter(passportAttributeDiff ->
                    expectedPassportAttributeDiff.getPassportAttribute().getCode().equals(passportAttributeDiff.getPassportAttribute().getCode())).findFirst().orElse(null);
            if (actualPassportAttributeDiff == null)
                Assert.fail("Attribute \"" + expectedPassportAttributeDiff.getPassportAttribute().getName() + "\" must be in diff");
            assertEquals(expectedPassportAttributeDiff.getFirstValue(), actualPassportAttributeDiff.getFirstValue());
            assertEquals(expectedPassportAttributeDiff.getSecondValue(), actualPassportAttributeDiff.getSecondValue());
        });
    }

}