package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierDataResponseType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.esnsi.jobs.EsnsiSyncJobUtils.PAGE_SIZE;

@DisallowConcurrentExecution
class GetDataPageJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        String pageProcessorId = jobDataMap.getString("id");
        if (!jobDataMap.getBoolean("busy_set")) {
            esnsiIntegrationDao.setPageProcessorBusy(pageProcessorId);
            jobDataMap.put("busy_set", true);
        } else {
            if (esnsiIntegrationDao.isPageProcessorIdle(pageProcessorId))
                shutdown();
        }
        String messageId = jobDataMap.getString("messageId");
        Map.Entry<GetClassifierDataResponseType, InputStream> data = esnsiSmevClient.getResponse(messageId, GetClassifierDataResponseType.class);
        if (data != null) {
            int revision = jobDataMap.getInt("revision");
            String tableName = jobDataMap.getString("tableName");
            GetClassifierStructureResponseType struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
            List<Object[]> batch = new ArrayList<>(PAGE_SIZE);
            EsnsiSyncJobUtils.EsnsiXmlDataFileReadUtil.read(batch::add, struct, data.getValue());
            esnsiIntegrationDao.insert(batch, tableName, pageProcessorId);
            esnsiSmevClient.acknowledge(messageId);
            batch.clear();
            shutdown();
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.GET_DATA;
    }

}
