package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.datastorage.temporal.service.StorageCodeService;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final String TEST_STORAGE_CODE = "test_storage_code";

    @InjectMocks
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private StorageCodeService storageCodeService;

    @Mock
    private SearchDataService searchDataService;

    @Test
    public void testSearchVersion() {

        RefBookVersionEntity testVersion = createTestVersion();

        when(versionRepository.findById(anyInt())).thenReturn(Optional.of(testVersion));
        when(storageCodeService.toStorageCode(any())).thenReturn(TEST_STORAGE_CODE);
        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>(0, Collections.emptyList(), new Criteria()));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilters(new HashSet<>());
        searchDataCriteria.setCommonFilter("commonFilter");
        versionService.search(1, searchDataCriteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(TEST_STORAGE_CODE, testVersion.getFromDate(), testVersion.getToDate(),
                new ArrayList<>(), toFieldSearchCriterias(searchDataCriteria.getAttributeFilters()), searchDataCriteria.getCommonFilter());
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
