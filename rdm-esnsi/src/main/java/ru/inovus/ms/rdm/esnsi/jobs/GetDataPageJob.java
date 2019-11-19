package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierDataResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
            GetClassifierStructureResponseType struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
            List<Object[]> batch = new ArrayList<>(PAGE_SIZE);
            EsnsiSyncJobUtils.EsnsiXmlDataFileReadUtil.read(batch::add, struct, data.getValue());
            esnsiIntegrationDao.insert(batch, tableName, pageProcessorId, this::interrupt);
            esnsiSmevClient.acknowledge(messageId);
            batch.clear();
        }
        return false;
    }

}
