package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    @InjectMocks
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private SearchDataService searchDataService;

    @Test
    public void testSearchVersion() {
        RefBookVersionEntity testVersion = createTestVersion();
        when(versionRepository.findById(anyInt())).thenReturn(Optional.of(testVersion));
        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>());
        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilter(new HashSet<>());
        searchDataCriteria.setCommonFilter("commonFilter");
        DataCriteria dataCriteria = new DataCriteria(TEST_STORAGE_CODE, testVersion.getFromDate(), testVersion.getToDate(), new ArrayList<>(),
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

}
