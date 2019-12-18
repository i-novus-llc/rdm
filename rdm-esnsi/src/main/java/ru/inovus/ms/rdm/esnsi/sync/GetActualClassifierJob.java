package ru.inovus.ms.rdm.esnsi.sync;

import org.quartz.*;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.*;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class GetActualClassifierJob extends AbstractEsnsiDictionaryProcessingJob {

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        String messageId = jobDataMap.getString(MESSAGE_ID_KEY);
        Map.Entry<ListClassifiersResponseType, InputStream> listClassifiersResponse = adapterClient.getResponse(messageId, ListClassifiersResponseType.class);
        if (listClassifiersResponse != null) {
            List<ClassifierDescriptorListType> descriptors = listClassifiersResponse.getKey().getClassifierDescriptor();
            ClassifierDescriptorListType classifierDescriptor = descriptors.stream().filter(descriptor -> descriptor.getCode().equals(classifierCode)).findAny().orElseThrow(() -> new RdmException("No classifier found in ESNSI with code " + classifierCode));
            Integer lastDownloadedRevision = esnsiLoadService.getLastVersionRevision(classifierCode);
            if (lastDownloadedRevision == null || lastDownloadedRevision < classifierDescriptor.getRevision()) {
                GetClassifierStructureRequestType getClassifierStructureRequestType = objectFactory.createGetClassifierStructureRequestType();
                getClassifierStructureRequestType.setRevision(classifierDescriptor.getRevision());
                getClassifierStructureRequestType.setCode(classifierCode);
                AcceptRequestDocument acceptRequestDocument = adapterClient.sendRequest(getClassifierStructureRequestType, UUID.randomUUID().toString());
                JobDetail job = JobBuilder.newJob(GetClassifierStructureJob.class).
                        withIdentity(GetClassifierStructureJob.class.getSimpleName(), classifierCode).
                        usingJobData(MESSAGE_ID_KEY, acceptRequestDocument.getMessageId()).
                        usingJobData(REVISION_KEY, classifierDescriptor.getRevision()).requestRecovery().
                        build();
                execSmevResponseResponseReadingJob(job);
            }
            return true;
        }
        return false;
    }

}
