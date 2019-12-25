package ru.inovus.ms.rdm.sync.service.change_data;

import org.quartz.*;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncJobContext;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.HashMap;
import java.util.List;

import static java.util.Collections.emptyList;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public final class RdmSyncExportDirtyRecordsToRdmJob implements Job {

    public static final String JOB_NAME = "ExportDirtyRecordsToRdm";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        RdmSyncDao dao = RdmSyncJobContext.getDao();
        RdmChangeDataClient changeDataClient = RdmSyncJobContext.getRdmChangeDataClient();
        int batchSize = RdmSyncJobContext.getExportToRdmBatchSize();
        int limit = batchSize;
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping vm : versionMappings) {
            int offset = 0;
            String table = vm.getTable();
            for (;;) {
                List<HashMap<String, Object>> batch = dao.getRecordsOfStateWithLimitOffset(table, limit, offset, true, RdmSyncLocalRowState.DIRTY);
                if (batch.isEmpty())
                    break;
                changeDataClient.changeData(vm.getCode(), batch, emptyList());
                offset += limit;
            }
        }
    }

}
