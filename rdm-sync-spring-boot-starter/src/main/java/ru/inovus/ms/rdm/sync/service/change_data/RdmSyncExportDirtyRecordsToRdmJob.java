package ru.inovus.ms.rdm.sync.service.change_data;

import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.HashMap;
import java.util.List;

import static java.util.Collections.emptyList;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public final class RdmSyncExportDirtyRecordsToRdmJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(RdmSyncExportDirtyRecordsToRdmJob.class);

    public static final String JOB_NAME = "ExportDirtyRecordsToRdm";

    @Value("${rdm_sync.export_from_local.batch_size:100}")
    private int batchSize;

    @Autowired private RdmChangeDataClient changeDataClient;
    @Autowired private RdmSyncDao dao;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        if (changeDataClient == null || dao == null)
            throw new JobExecutionException("Cannot autowire instances of RdmChangeDataClient and/or RdmSyncDao. Setup your quartz scheduler with AutowiringSpringBeanJobFactory.", false);
        int limit = batchSize;
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping vm : versionMappings) {
            int n = 0;
            int offset = 0;
            String table = vm.getTable();
            for (;;) {
                List<HashMap<String, Object>> batch = dao.getRecordsOfStateWithLimitOffset(table, limit, offset, true, RdmSyncLocalRowState.DIRTY);
                if (batch.isEmpty())
                    break;
                n += batch.size();
                changeDataClient.changeData(vm.getCode(), batch, emptyList());
                offset += limit;
            }
            logger.info("{} records were marked as dirty in {} local table.", n, vm.getTable());
        }
    }



}
