package ru.inovus.ms.rdm.sync.service.change_data;

import org.quartz.*;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncJobContext;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static ru.inovus.ms.rdm.sync.service.change_data.RdmSyncChangeDataUtils.INTERNAL_TAG;
import static ru.inovus.ms.rdm.sync.service.change_data.RdmSyncChangeDataUtils.reindex;

@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public final class RdmSyncExportDirtyRecordsToRdmJob implements Job {

    public static final String NAME = "ExportDirtyRecordsToRdm";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        RdmSyncDao dao = RdmSyncJobContext.getDao();
        RdmChangeDataClient changeDataClient = RdmSyncJobContext.getRdmChangeDataClient();
        int limit = RdmSyncJobContext.getExportToRdmBatchSize();
        List<VersionMapping> versionMappings = dao.getVersionMappings();
        for (VersionMapping vm : versionMappings) {
            int offset = 0;
            String table = vm.getTable();
            List<FieldMapping> fieldMappings = dao.getFieldMapping(vm.getCode());
            for (;;) {
                List<HashMap<String, Object>> batch = dao.getRecordsOfState(table, limit, offset, RdmSyncLocalRowState.DIRTY);
                if (batch.isEmpty())
                    break;
                batch.add(INTERNAL_TAG);
                changeDataClient.changeData(vm.getCode(), batch, emptyList(), t -> {
                    Map<String, Object> m = new HashMap<>(t);
                    reindex(fieldMappings, m);
                    return m;
                });
                offset += limit;
            }
        }
    }

}
