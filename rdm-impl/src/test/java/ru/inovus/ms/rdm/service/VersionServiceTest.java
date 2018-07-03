package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.model.VersionCriteria;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    @InjectMocks
    VersionServiceImpl versionService;


    @Mock
    RefBookVersionRepository versionRepository;

    @Mock
    SearchDataService searchDataService;

    @Mock
    FieldFactory fieldFactory;

    @Test
    public void testSearchVersion() {
        RefBookVersionEntity testVersion = createTestVersion();
        when(versionRepository.findOne(anyInt())).thenReturn(testVersion);
        when(fieldFactory.createField(any(), any())).thenReturn(null);
        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>());
        Date bdate = testVersion.getFromDate() != null ? Date.from(testVersion.getFromDate().atZone(ZoneOffset.UTC).toInstant()) : null;
        Date edate = testVersion.getToDate() != null ? Date.from(testVersion.getToDate().atZone(ZoneOffset.UTC).toInstant()) : null;
        VersionCriteria versionCriteria = new VersionCriteria();
        versionCriteria.setFieldFilter(new ArrayList<FieldSearchCriteria>());
        versionCriteria.setCommonFilter("commonFilter");
        DataCriteria dataCriteria = new DataCriteria(TEST_STORAGE_CODE, bdate, edate, new ArrayList<Field>(),
                versionCriteria.getFieldFilter(), versionCriteria.getCommonFilter());
        versionService.search(1, versionCriteria);
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

}
