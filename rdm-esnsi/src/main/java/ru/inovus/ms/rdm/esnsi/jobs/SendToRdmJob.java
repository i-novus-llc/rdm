package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;

@DisallowConcurrentExecution
class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt("revision");
        String fileName = classifierCode + "-" + revision + ".xml";
        File f = new File(fileName);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            GetClassifierStructureResponseType struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
            EsnsiSyncJobUtils.XmlDataCreator dataCreator = new EsnsiSyncJobUtils.XmlDataCreator(out, struct);
            dataCreator.init();
            esnsiIntegrationDao.readRows(dataCreator, classifierCode, revision);
            dataCreator.end();
        }
        String rdmRestUrl = getProperty("rdm.rest.url");
        String fileStorageService = rdmRestUrl + "/fileStorage/save";
        String uri = UriComponentsBuilder.fromHttpUrl(fileStorageService).queryParam("fileName", fileName).build().toUriString();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        Resource resource = new InputStreamResource(new BufferedInputStream(new FileInputStream(f)));
        ResponseEntity<FileModel> fileModel = restTemplate.exchange(
            uri,
            HttpMethod.POST,
            new HttpEntity<>(resource, httpHeaders),
            FileModel.class,
            fileName
        );
        FileModel body = fileModel.getBody();
        String draftService = rdmRestUrl + "/draft/createByFile";
        restTemplate.postForObject(draftService, null, String.class, Map.of("path", body.path, "name", body.name));
        esnsiIntegrationDao.updateLastDownloaded(classifierCode, revision, Timestamp.from(Instant.now(Clock.systemUTC())));
        return true;
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
