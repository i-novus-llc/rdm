package ru.inovus.ms.rdm.esnsi.sync;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.FileModel;
import ru.inovus.ms.rdm.api.model.draft.Draft;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.FileStorageService;
import ru.inovus.ms.rdm.api.service.PublishService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.inovus.ms.rdm.esnsi.file_gen.RdmXmlFileGenerator;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.esnsi.EsnsiLoaderDao.FIELD_PREFIX;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class SendToRdmJob extends AbstractEsnsiDictionaryProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(SendToRdmJob.class);

    private static final String TEMP_FILE_PREFIX = "amatmpfiledeletemeplease_";

    private static final String DRAFT_ID_KEY = "draftId";

    @Autowired
    private RefBookService refBookService;

    @Autowired
    private DraftService draftService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PublishService publishService;

    private String fileName;
    private int revision;
    private String refBookCode;

    @Override
    boolean execute0(JobExecutionContext context) throws Exception {
        revision = jobDataMap.getInt(REVISION_KEY);
        GetClassifierStructureResponseType struct = esnsiLoadService.getClassifierStruct(classifierCode, revision);
        refBookCode = "ESNSI-" + struct.getClassifierDescriptor().getPublicId();
        fileName = checkFileExists();
        if (fileName == null) {
            fileName = createFile();
            jobDataMap.put(TEMP_FILE_PREFIX + fileName, true);
        }
        FileModel fileModel = uploadToFileStorage();
        int draftId = -1;
        Draft draft = null;
        if (jobDataMap.containsKey(DRAFT_ID_KEY))
            draftId = jobDataMap.getInt(DRAFT_ID_KEY);
        else {
            Integer id = checkForExistance();
            if (id == null)
                draft = draftService.create(fileModel);
            else
                draft = draftService.create(id, fileModel);
        }
        if (draftId == -1) {
            draftId = draft == null ? -1 : draft.getId();
            if (draftId == -1) {
                Integer draftIdByRefBookCode = draftService.getIdByRefBookCode(refBookCode);
                if (draftIdByRefBookCode == null) {
                    logger.warn("Unable to fetch draft from RDM. Publication of draft failed. If the draft is still contained in the RDM, publish it manually.");
                    return true;
                } else
                    draftId = draftIdByRefBookCode;
            }
        }
        jobDataMap.put(DRAFT_ID_KEY, draftId);
        publishService.publish(draftId, null, null, null, false);
        esnsiLoadService.setClassifierRevisionAndLastUpdated(classifierCode, revision, Timestamp.valueOf(LocalDateTime.now(Clock.systemUTC())));
        return true;
    }

    private String createFile() throws IOException, XMLStreamException {
        String newFileName = System.currentTimeMillis() + ".xml";
        File f = new File(newFileName);
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(f))) {
            GetClassifierStructureResponseType struct = esnsiLoadService.getClassifierStruct(classifierCode, revision);
            int primaryKeyFieldSerialNumber = struct.getAttributeList().stream().filter(ClassifierAttribute::isKey).findFirst().map(ClassifierAttribute::getOrder).orElseThrow(() -> new RdmException("Can't find primary key in this classifier structure.")) + 1;
            String primaryKeyFieldName = FIELD_PREFIX + primaryKeyFieldSerialNumber;
            RdmXmlFileGenerator generator = EsnsiSyncJobUtils.RdmXmlFileGeneratorProvider.get(out, struct, new Iterator<>() {

                String lastSeenId = "";
                Iterator<Map<String, Object>> it = null;

                @Override
                public boolean hasNext() {
                    if (it == null || !it.hasNext()) {
                        List<Map<String, Object>> classifierData = esnsiLoadService.getClassifierData(struct.getClassifierDescriptor().getPublicId(), revision, lastSeenId, primaryKeyFieldSerialNumber);
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

            }, getProperty("esnsi.sync.date-formats"));
            generator.init();
            generator.fetchData();
            generator.end();
        }
        jobDataMap.put(TEMP_FILE_PREFIX + newFileName, true);
        return newFileName;
    }

    private FileModel uploadToFileStorage() throws FileNotFoundException {
        return fileStorageService.save(new BufferedInputStream(new FileInputStream(fileName)), fileName);
    }

    private Integer checkForExistance() {
        Integer id = null;
        try {
            id = refBookService.getId(refBookCode);
        } catch (Exception e) {
            logger.info("RefBook with code {} is not exists.", refBookCode, e);
        }
        return id;
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
