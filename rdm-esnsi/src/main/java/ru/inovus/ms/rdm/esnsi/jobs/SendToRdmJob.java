package ru.inovus.ms.rdm.esnsi.jobs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.inovus.ms.rdm.esnsi.file_gen.RdmXmlFileGenerator;

import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(SendToRdmJob.class);

    private static final String PREFIX = "amatmpfiledeletemeplease_";

    @Autowired
    private RestTemplate restTemplate;

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        int revision = jobDataMap.getInt(REVISION_KEY);
        String fileName = System.currentTimeMillis() + ".xml";
        jobDataMap.put(PREFIX + fileName, true);
        File f = new File(fileName);
        f.deleteOnExit();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            GetClassifierStructureResponseType struct = esnsiLoadService.getClassifierStruct(classifierCode, revision);
            int primaryKeyFieldSerialNumber = struct.getAttributeList().stream().filter(ClassifierAttribute::isKey).findFirst().map(ClassifierAttribute::getOrder).get() + 1;
            String primaryKeyFieldName = "field_" + primaryKeyFieldSerialNumber;
            RdmXmlFileGenerator generator = EsnsiSyncJobUtils.RdmXmlFileGeneratorProvider.get(out, struct, new Iterator<>() {

                String lastSeenId = "";
                Iterator<Map<String, Object>> it;

                @Override
                public boolean hasNext() {
                    if (!it.hasNext()) {
                        List<Map<String, Object>> classifierData = esnsiLoadService.getClassifierData(classifierCode, revision, lastSeenId, primaryKeyFieldSerialNumber);
                        it = classifierData.iterator();
                    }
                    return it.hasNext();
                }

                @Override
                public Map<String, Object> next() {
                    Map<String, Object> next = it.next();
                    lastSeenId = (String) next.get(primaryKeyFieldName);
                    return next;
                }

            });
            generator.init();
            generator.fetchData();
            generator.end();
        }
        String rdmRestUrl = getProperty("rdm.backend.path");
        String fileStorageService = rdmRestUrl + "/fileStorage/save";
        String uri = UriComponentsBuilder.fromHttpUrl(fileStorageService).queryParam("fileName", fileName).build().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        Resource resource = new FileSystemResource(f);
        MultiValueMap<String, Object> body
                = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpEntity<?> requestEntity
                = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
        String fileModel = response.getBody();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(fileModel);
        String draftService = rdmRestUrl + "/draft/createByFile";
        headers.setContentType(MediaType.APPLICATION_JSON);
        requestEntity = new HttpEntity<>(jsonNode.toString(), headers);
        restTemplate.postForEntity(draftService, requestEntity, String.class);
        esnsiLoadService.setClassifierRevisionAndLastUpdated(classifierCode, revision, Timestamp.from(Instant.now(Clock.systemUTC())));
        return true;
    }

    @Override
    void afterInterrupt() {
        String[] keys = jobDataMap.getKeys();
        for (String key : keys) {
            if (key.startsWith(PREFIX)) {
                File tmpFile = new File(key.substring(PREFIX.length()));
                if (tmpFile.exists()) {
                    try {
                        Files.delete(tmpFile.toPath());
                    } catch (IOException e) {
                        logger.warn("Temporary file {} was not deleted. Please, do these manually to prevent memory leak.", tmpFile.getAbsolutePath(), e);
                    }
                }
            }
        }
    }
}
