package ru.inovus.ms.rdm.sync.service.change_data;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.data.domain.Page;
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
            String deletedKey = vm.getDeletedField();
            for (;;) {
                Page<HashMap<String, Object>> dirtyBatch = dao.getData(table, vm.getPrimaryField(), limit, offset, RdmSyncLocalRowState.DIRTY, null);
                if (dirtyBatch.getContent().isEmpty())
                    break;
                List<HashMap<String, Object>> addUpdate = new ArrayList<>();
                List<HashMap<String, Object>> delete = new ArrayList<>();
                for (HashMap<String, Object> map : dirtyBatch.getContent()) {
                    Boolean deletedVal = (Boolean) map.get(deletedKey);
                    if (deletedVal == null || !deletedVal)
                        addUpdate.add(map);
                    else
                        delete.add(map);
                }
                addUpdate.add(INTERNAL_TAG);
                changeDataClient.changeData(vm.getCode(), addUpdate, delete, record -> {
                    Map<String, Object> map = new HashMap<>(record);
                    reindex(fieldMappings, map);
                    return map;
                });
                offset += limit;
            }
        }
    }

}
