package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.AcceptRequestDocument;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierRecordsCountRequestType;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@DisallowConcurrentExecution
class GetClassifierStructureJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(GetClassifierStructureJob.class);

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt("revision");
        String messageId = jobDataMap.getString("messageId");
        Map.Entry<GetClassifierStructureResponseType, InputStream> getClassifierStructureResponseType = esnsiSmevClient.getResponse(messageId, GetClassifierStructureResponseType.class);
        if (getClassifierStructureResponseType != null) {
            GetClassifierStructureResponseType struct = getClassifierStructureResponseType.getKey();
            if (struct.getAttributeList().stream().noneMatch(ClassifierAttribute::isKey)) {
                logger.warn("Classifier with code {} doesn't have primary key. Shutting down.", classifierCode);
                interrupt();
                return;
            }
            esnsiIntegrationDao.createEsnsiVersionDataTableAndRemovePreviousIfNecessaryAndSaveStruct(struct);
            GetClassifierRecordsCountRequestType getClassifierRecordsCountRequestType = objectFactory.createGetClassifierRecordsCountRequestType();
            getClassifierRecordsCountRequestType.setCode(classifierCode);
            getClassifierRecordsCountRequestType.setRevision(revision);
            AcceptRequestDocument acceptRequestDocument = esnsiSmevClient.sendRequest(getClassifierRecordsCountRequestType, UUID.randomUUID().toString());
            JobDetail job = JobBuilder.newJob(GetClassifierRecordsCountJob.class).
                            usingJobData("revision", revision).
                            usingJobData("messageId", acceptRequestDocument.getMessageId()).requestRecovery().
                            withIdentity(GetClassifierRecordsCountJob.class.getSimpleName(), classifierCode).build();
            execSmevResponseResponseReadingJob(job);
            interrupt();
        }
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.GET_STRUCTURE;
    }

}
