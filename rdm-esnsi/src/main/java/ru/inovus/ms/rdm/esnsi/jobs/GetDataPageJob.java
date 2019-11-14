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
        int pageProcessorId = jobDataMap.getInt("id");
        int revision = jobDataMap.getInt("revision");
        if (!jobDataMap.getBoolean("busy_set")) {
            esnsiIntegrationDao.setPageProcessorBusy(classifierCode, revision, pageProcessorId);
            jobDataMap.put("busy_set", true);
        }
        String messageId = jobDataMap.getString("messageId");
        Map.Entry<GetClassifierDataResponseType, InputStream> data = esnsiSmevClient.getResponse(messageId, GetClassifierDataResponseType.class);
        if (data != null) {
            String tableName = jobDataMap.getString("tableName");
            GetClassifierStructureResponseType struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
            List<Object[]> batch = new ArrayList<>(PAGE_SIZE);
            EsnsiSyncJobUtils.EsnsiXmlDataFileReadUtil.read(batch::add, struct, data.getValue());
            esnsiIntegrationDao.insert(batch, tableName, classifierCode, revision, pageProcessorId, this::shutdown);
            esnsiSmevClient.acknowledge(messageId);
            batch.clear();
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.GET_DATA;
    }

}
