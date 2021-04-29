package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.Criteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.BaseStrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;

@RunWith(MockitoJUnitRunner.class)
public class VersionServiceTest {

    private static final int REFBOOK_ID = 1;
    private static final String REF_BOOK_CODE = "test_refbook";

    private static final int VERSION_ID = 2;
    private static final String STORAGE_CODE = "test_storage_code";

    @InjectMocks
    private VersionServiceImpl versionService;

    @Mock
    private RefBookVersionRepository versionRepository;

    @Mock
    private SearchDataService searchDataService;

    @Before
    public void setUp() throws Exception {

        final StrategyLocator strategyLocator = new BaseStrategyLocator(getStrategies());
        FieldSetter.setField(versionService, VersionServiceImpl.class.getDeclaredField("strategyLocator"), strategyLocator);
    }

    @Test
    public void testSearchVersion() {

        RefBookVersionEntity versionEntity = createVersionEntity();

        when(versionRepository.findById(anyInt())).thenReturn(Optional.of(versionEntity));
        when(searchDataService.getPagedData(any())).thenReturn(new CollectionPage<>(0, emptyList(), new Criteria()));

        SearchDataCriteria searchDataCriteria = new SearchDataCriteria();
        searchDataCriteria.setAttributeFilters(new HashSet<>());
        searchDataCriteria.setCommonFilter("commonFilter");
        versionService.search(VERSION_ID, searchDataCriteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(STORAGE_CODE, versionEntity.getFromDate(), versionEntity.getToDate(),
                new ArrayList<>(), toFieldSearchCriterias(searchDataCriteria.getAttributeFilters()), searchDataCriteria.getCommonFilter());
        dataCriteria.setPage(searchDataCriteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        dataCriteria.setSize(searchDataCriteria.getPageSize());

        verify(searchDataService).getPagedData(eq(dataCriteria));
    }

    private RefBookVersionEntity createVersionEntity() {

        RefBookVersionEntity entity = new RefBookVersionEntity();
        entity.setId(VERSION_ID);
        entity.setRefBook(createRefBookEntity());
        entity.setStructure(new Structure());
        entity.setStorageCode(STORAGE_CODE);
        entity.setStatus(RefBookVersionStatus.PUBLISHED);
        entity.setFromDate(LocalDateTime.now());

        return entity;
    }

    private RefBookEntity createRefBookEntity() {

        RefBookEntity entity = new RefBookEntity();
        entity.setId(REFBOOK_ID);
        entity.setCode(REF_BOOK_CODE);

        return entity;
    }

    private Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> getStrategies() {

        Map<RefBookTypeEnum, Map<Class<? extends Strategy>, Strategy>> result = new HashMap<>();
        result.put(RefBookTypeEnum.DEFAULT, getDefaultStrategies());

        return result;
    }

    private Map<Class<? extends Strategy>, Strategy> getDefaultStrategies() {

        return new HashMap<>();
    }
}
