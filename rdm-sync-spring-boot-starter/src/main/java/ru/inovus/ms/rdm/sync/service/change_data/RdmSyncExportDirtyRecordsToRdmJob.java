package ru.inovus.ms.rdm.sync.service.change_data;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import ru.inovus.ms.rdm.sync.model.FieldMapping;
import ru.inovus.ms.rdm.sync.model.VersionMapping;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;
import ru.inovus.ms.rdm.sync.service.RdmSyncJobContext;
import ru.inovus.ms.rdm.sync.service.RdmSyncLocalRowState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.sync.service.change_data.RdmSyncChangeDataUtils.INTERNAL_TAG;
import static ru.inovus.ms.rdm.sync.service.change_data.RdmSyncChangeDataUtils.reindex;

@DisallowConcurrentExecution
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
            String deleted = vm.getDeletedField();
            for (;;) {
                List<HashMap<String, Object>> batch = dao.getRecordsOfState(table, limit, offset, RdmSyncLocalRowState.DIRTY);
                if (batch.isEmpty())
                    break;
                List<HashMap<String, Object>> addUpdate = new ArrayList<>();
                List<HashMap<String, Object>> delete = new ArrayList<>();
                for (HashMap<String, Object> map : batch) {
                    Boolean b = (Boolean) map.get(deleted);
                    if (b == null || !b)
                        addUpdate.add(map);
                    else
                        delete.add(map);
                }
                addUpdate.add(INTERNAL_TAG);
                changeDataClient.changeData(vm.getCode(), addUpdate, delete, t -> {
                    Map<String, Object> m = new HashMap<>(t);
                    reindex(fieldMappings, m);
                    return m;
                });
                offset += limit;
            }
        }
    }

}
