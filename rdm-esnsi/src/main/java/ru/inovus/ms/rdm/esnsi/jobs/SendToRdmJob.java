package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.toMultiValueMap;

@DisallowConcurrentExecution
class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    void execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt("revision");
        String fileName = classifierCode + "-" + revision + ".xml";
        File f = new File("/" + fileName);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(f));
        GetClassifierStructureResponseType struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
        EsnsiSyncJobUtils.XmlDataCreator dataCreator = new EsnsiSyncJobUtils.XmlDataCreator(out, struct);
        dataCreator.init();
        esnsiIntegrationDao.readRows(dataCreator, classifierCode, revision);
        dataCreator.end();
        String rdmRestUrl = getProperty("rdm.rest.url");
        String fileStorageService = rdmRestUrl + "/fileStorage/save";
        Resource resource = new InputStreamResource(new BufferedInputStream(new FileInputStream(f)));
        ResponseEntity<FileModel> fileModel = restTemplate.exchange(
            fileStorageService,
            HttpMethod.POST,
            new HttpEntity<>(resource, toMultiValueMap(Map.of("fileName", singletonList(fileName)))),
            FileModel.class
        );
        FileModel body = fileModel.getBody();
        String draftService = rdmRestUrl + "/draft/createByFile";
        restTemplate.postForObject(draftService, null, String.class, body);
        esnsiIntegrationDao.updateLastDownloaded(classifierCode, revision, Timestamp.from(Instant.now(Clock.systemUTC())));
        unschedule();
    }

    @Override
    ClassifierProcessingStage stage() {
        return ClassifierProcessingStage.SENDING_TO_RDM;
    }

    private static class FileModel {
        private String path;
        private String name;
        public FileModel() {}
        public FileModel(String path, String name) {
            this.path = path;
            this.name = name;
        }
    }

}
