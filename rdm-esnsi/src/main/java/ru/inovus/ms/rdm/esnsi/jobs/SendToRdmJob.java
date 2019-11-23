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
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.inovus.ms.rdm.esnsi.ClassifierProcessingStage;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;

import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public
class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(SendToRdmJob.class);

    private static final String TEMP_FILE_PREFIX = "amatmpfiledeletemeplease_";

    @Autowired
    private RestTemplate restTemplate;

    private String rdmRestUrl;
    private String fileName;
    private GetClassifierStructureResponseType struct;
    private int revision;
    private String dictionaryCode;

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        revision = jobDataMap.getInt(REVISION_KEY);
        struct = esnsiIntegrationDao.getStruct(classifierCode, revision);
        dictionaryCode = "ESNSI-" + struct.getClassifierDescriptor().getPublicId();
        rdmRestUrl = getProperty("rdm.backend.path");
        fileName = checkFileExists();
        if (fileName == null) {
            fileName = createFile();
            jobDataMap.put(TEMP_FILE_PREFIX + fileName, true);
        }
        String fileModel = uploadToFileStorage();
        String draftService = rdmRestUrl + "/draft";
        Integer id = checkForExistance();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<?> requestEntity = new HttpEntity<>(fileModel, headers);
        String uri = draftService + "/createByFile";
        if (id != null)
            uri += "/" + id;
        ResponseEntity<String> draft = null;
        try {
            draft = restTemplate.postForEntity(uri, requestEntity, String.class);
        } catch (Exception ignored) {
//          Возможно мы уже постили файл
        }
        int draftId = -1;
        if (draft != null && draft.getStatusCode() == HttpStatus.OK) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(draft.getBody());
            draftId = jsonNode.get("id").asInt();
        } else {
            ResponseEntity<Integer> draftIdEntity = restTemplate.getForEntity(draftService + "/getIdByRefBookCode/" + dictionaryCode, Integer.class);
            if (draftIdEntity.getBody() != null)
                draftId = draftIdEntity.getBody();
        }
        if (draftId == -1) {
            logger.warn("Unable to fetch draft id from RDM. Publication of draft failed. If the draft is still contained in the RDM, publish it manually.");
            esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE, () -> {});
        } else {
            requestEntity = new HttpEntity<>(headers);
            String publishService = rdmRestUrl + "/publish";
            ResponseEntity<Void> v0id = restTemplate.exchange(publishService + "/" + draftId, HttpMethod.POST, requestEntity, Void.class);
            if (v0id.getStatusCode() == HttpStatus.NO_CONTENT)
                esnsiIntegrationDao.updateLastDownloaded(classifierCode, revision, Timestamp.from(Instant.now(Clock.systemUTC())));
            else {
                logger.warn("Publication failed. Do this manually please.");
                esnsiIntegrationDao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE, () -> {});
            }
        }
        return true;
    }

    private String createFile() throws IOException {
        String s = System.currentTimeMillis() + ".xml";
        File f = new File(s);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            EsnsiSyncJobUtils.XmlDataCreator dataCreator = new EsnsiSyncJobUtils.XmlDataCreator(out, struct);
            dataCreator.init();
            esnsiIntegrationDao.readRows(dataCreator, classifierCode, revision);
            dataCreator.end();
        }
        return s;
    }

    private String uploadToFileStorage() {
        String fileStorageService = rdmRestUrl + "/fileStorage/save";
        String uri = UriComponentsBuilder.fromHttpUrl(fileStorageService).queryParam("fileName", fileName).build().toUriString();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        Resource resource = new FileSystemResource(new File(fileName));
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        HttpEntity<?> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);
        return response.getBody();
    }

    private Integer checkForExistance() {
        String refBookService = rdmRestUrl + "/refBook";
        String uri = UriComponentsBuilder.fromHttpUrl(refBookService + "/code/" + dictionaryCode).build().toUriString();
        try {
            return restTemplate.getForEntity(uri, Integer.class).getBody();
        } catch (HttpClientErrorException.NotFound e404) {
            return null;
        }
    }

    private String checkFileExists() {
        for (String key : jobDataMap.getKeys()) {
            if (key.startsWith(TEMP_FILE_PREFIX))
                return key.replaceFirst(TEMP_FILE_PREFIX, "");
        }
        return null;
    }

    @Override
    void afterInterrupt() {
        String[] keys = jobDataMap.getKeys();
        for (String key : keys) {
            if (key.startsWith(TEMP_FILE_PREFIX)) {
                File tmpFile = new File(key.substring(TEMP_FILE_PREFIX.length()));
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
