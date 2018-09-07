package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.PassportAttributeDiff;
import ru.inovus.ms.rdm.model.PassportDiff;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";
    private static final String PASSPORT_ATTRIBUTE_FULL_NAME = "TEST_fullName";
    private static final String PASSPORT_ATTRIBUTE_SHORT_NAME = "TEST_shortName";
    private static final String PASSPORT_ATTRIBUTE_ANNOTATION = "TEST_annotation";
    private static final String PASSPORT_ATTRIBUTE_GROUP = "TEST_group";
    private static final String PASSPORT_ATTRIBUTE_TYPE = "TEST_type";

    @InjectMocks
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private PassportAttributeRepository passportAttributeRepository;

    @Mock
    private SearchDataService searchDataService;

    @Test
    public void testSearchVersion() {
        RefBookVersionEntity testVersion = createTestVersion();
        when(versionRepository.findOne(anyInt())).thenReturn(testVersion);
        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>());
        Date bdate = testVersion.getFromDate() != null ? Date.from(testVersion.getFromDate().atZone(ZoneId.systemDefault()).toInstant()) : null;
        Date edate = testVersion.getToDate() != null ? Date.from(testVersion.getToDate().atZone(ZoneId.systemDefault()).toInstant()) : null;
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilter(new ArrayList<>());
        searchDataCriteria.setCommonFilter("commonFilter");
        DataCriteria dataCriteria = new DataCriteria(TEST_STORAGE_CODE, bdate, edate, new ArrayList<>(),
                ConverterUtil.getFieldSearchCriteriaList(searchDataCriteria.getAttributeFilter()), searchDataCriteria.getCommonFilter());
        versionService.search(1, searchDataCriteria);
        verify(searchDataService).getPagedData(eq(dataCriteria));
    }

    private RefBookVersionEntity createTestVersion() {
        RefBookVersionEntity testVersion = new RefBookVersionEntity();
        testVersion.setId(1);
        testVersion.setStorageCode(TEST_STORAGE_CODE);
        testVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        testVersion.setStructure(new Structure());
        testVersion.setFromDate(LocalDateTime.now());
        return testVersion;
    }

    @Test
    public void testComparePassports() {
        RefBookVersionEntity sourceVersion = new RefBookVersionEntity();
        sourceVersion.setId(2);
        sourceVersion.setStatus(RefBookVersionStatus.PUBLISHED);
        PassportAttributeEntity passportAttributeFullName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_FULL_NAME, "Полное название");
        PassportAttributeEntity passportAttributeShortName = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_SHORT_NAME, "Краткое название");
        PassportAttributeEntity passportAttributeAnnotation= new PassportAttributeEntity(PASSPORT_ATTRIBUTE_ANNOTATION, "Аннотация");
        PassportAttributeEntity passportAttributeGroup = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_GROUP, "Группа");
        PassportAttributeEntity passportAttributeType = new PassportAttributeEntity(PASSPORT_ATTRIBUTE_TYPE, "Тип");
        List<PassportAttributeEntity> passportAttributeEntities = Arrays.asList(passportAttributeFullName, passportAttributeShortName, passportAttributeAnnotation, passportAttributeGroup, passportAttributeType);

        Set<PassportValueEntity> sourcePassportValues = new HashSet<>();
        sourcePassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name", sourceVersion));
        sourcePassportValues.add(new PassportValueEntity(passportAttributeShortName, "short_name", sourceVersion));
        sourcePassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", sourceVersion));
        sourcePassportValues.add(new PassportValueEntity(passportAttributeType, null, sourceVersion));

        sourceVersion.setPassportValues(sourcePassportValues);

        RefBookVersionEntity targetVersion = new RefBookVersionEntity();
        targetVersion.setId(3);
        targetVersion.setStatus(RefBookVersionStatus.PUBLISHED);

        Set<PassportValueEntity> targetPassportValues = new HashSet<>();
        targetPassportValues.add(new PassportValueEntity(passportAttributeFullName, "full_name_upd", targetVersion));
        targetPassportValues.add(new PassportValueEntity(passportAttributeAnnotation, "annotation", targetVersion));
        targetPassportValues.add(new PassportValueEntity(passportAttributeGroup, "group", targetVersion));
        targetPassportValues.add(new PassportValueEntity(passportAttributeType, null, targetVersion));

        targetVersion.setPassportValues(targetPassportValues);

        when(versionRepository.getOne(sourceVersion.getId())).thenReturn(sourceVersion);
        when(versionRepository.getOne(targetVersion.getId())).thenReturn(targetVersion);
        when(passportAttributeRepository.findAllByComparableIsTrue()).thenReturn(passportAttributeEntities);

        PassportDiff passportDiff = versionService.comparePassports(sourceVersion.getId(), targetVersion.getId());
        assertEquals(passportAttributeEntities.size(), passportDiff.getPassportAttributeDiffs().size());
        validatePassportAttributeDiffs(passportDiff.getPassportAttributeDiffs(), sourcePassportValues, targetPassportValues);
    }

    private void validatePassportAttributeDiffs(List<PassportAttributeDiff> passportAttributeDiffs, Set<PassportValueEntity> sourcePassportValues, Set<PassportValueEntity> targetPassportValues) {
        passportAttributeDiffs.forEach(passportAttributeDiff -> {
            PassportValueEntity sourcePassportValue = sourcePassportValues.stream().filter(passportValueEntity -> passportValueEntity.getAttribute().getName().equals(passportAttributeDiff.getAttributeName())).findFirst().orElse(null);
            PassportValueEntity targetPassportValue = targetPassportValues.stream().filter(passportValueEntity -> passportValueEntity.getAttribute().getName().equals(passportAttributeDiff.getAttributeName())).findFirst().orElse(null);
            assertTrue(compareValues(sourcePassportValue, passportAttributeDiff.getOldValue()));
            assertTrue(compareValues(targetPassportValue, passportAttributeDiff.getNewValue()));
        });
    }

    private boolean compareValues(PassportValueEntity passportValue, String value) {
        if (passportValue != null && passportValue.getValue() != null)
            return value != null && passportValue.getValue().equals(value);
        else
            return value == null;
    }

}
