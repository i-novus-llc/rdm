package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierDataResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.InputStream;
import java.util.Map;

import static ru.inovus.ms.rdm.esnsi.jobs.EsnsiSyncJobUtils.PAGE_SIZE;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class GetDataPageJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String pageProcessorId = jobDataMap.getString("id");
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<GetClassifierDataResponseType, InputStream> data = esnsiSmevClient.getResponse(messageId, GetClassifierDataResponseType.class);
        if (data != null) {
            int revision = jobDataMap.getInt(REVISION_KEY);
            String tableName = jobDataMap.getString("tableName");
            GetClassifierStructureResponseType struct = esnsiLoadService.getClassifierStruct(classifierCode, revision);
            Map<String, String>[] batch = new Map[PAGE_SIZE];
            var ref = new Object() {
                int i = 0;
            };
            EsnsiSyncJobUtils.EsnsiXmlDataFileReadUtil.read(row -> batch[ref.i++] = row, struct, data.getValue());
            esnsiLoadService.insertAtomically(batch, tableName, revision, pageProcessorId, this::interrupt);
            esnsiSmevClient.acknowledge(messageId);
        }
        return false;
    }

}
