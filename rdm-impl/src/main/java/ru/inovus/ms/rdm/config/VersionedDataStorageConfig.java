package ru.inovus.ms.rdm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import ru.i_novus.platform.datastorage.temporal.service.CompareDataService;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.service.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * Created by tnurdinov on 25.05.2018.
 */

@Configuration
public class VersionedDataStorageConfig {

    @PersistenceContext
    private EntityManager entityManager;


    @Bean
    public DataDao dataDao() {
        return new DataDao(entityManager);
    }

    @Bean
    public SearchDataService getSearchDataService() {
        SearchDataServiceImpl searchDataService = new SearchDataServiceImpl();
        searchDataService.setDataDao(dataDao());
        return searchDataService;
    }

    @Bean
    public DraftDataService getDraftDataService() {
        DraftDataServiceImpl draftDataService = new DraftDataServiceImpl();
        draftDataService.setDataDao(dataDao());
        return draftDataService;
    }

    @Bean
    public DropDataService getDropDataService() {
        DropDataServiceImpl service = new DropDataServiceImpl();
        service.setDataDao(dataDao());
        return service;
    }

    @Bean
    public CompareDataService getCompareDataService() {
        CompareDataServiceImpl service = new CompareDataServiceImpl();
        service.setDataDao(dataDao());
        return service;
    }

}
