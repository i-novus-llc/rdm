package ru.inovus.ms.rdm.esnsi.jobs;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@DisallowConcurrentExecution
class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt("revision");
        String fileName = System.currentTimeMillis() + ".xml";
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
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        Resource resource = new FileSystemResource(f);
        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpEntity<MultiValueMap<String, Object>> requestEntity
                = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
//        String fileModel = response.getBody();
//        String draftService = rdmRestUrl + "/draft/createByFile";
//        restTemplate.postForObject(draftService, null, String.class, Map.of("path", fileModel.path, "name", fileModel.name));
        esnsiIntegrationDao.updateLastDownloaded(classifierCode, revision, Timestamp.from(Instant.now(Clock.systemUTC())));
        f.deleteOnExit();
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
