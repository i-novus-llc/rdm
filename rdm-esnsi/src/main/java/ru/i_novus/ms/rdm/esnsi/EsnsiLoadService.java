package ru.i_novus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.i_novus.ms.rdm.esnsi.api.CnsiResponse;
import ru.i_novus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.i_novus.ms.rdm.esnsi.api.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static ru.i_novus.ms.rdm.esnsi.EsnsiLoaderDao.FIELD_PREFIX;

@Component
public class EsnsiLoadService {

    private static final Logger logger = LoggerFactory.getLogger(EsnsiLoadService.class);

    private static final ObjectFactory OBJECT_FACTORY = new ObjectFactory();

    private static final int PAGE_SIZE = 100;

    private static final JAXBContext STRUCT_CTX;

    static {
        try {
            STRUCT_CTX = JAXBContext.newInstance(CnsiResponse.class);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }

    @Autowired
    private EsnsiLoaderDao dao;

    @Transactional
    public Integer getLastVersionRevision(String code) {
        return dao.getLastVersionRevision(code);
    }

    @Transactional
    public ClassifierProcessingStage getClassifierProcessingStage(String code) {
        ClassifierProcessingStage stage = dao.getClassifierProcessingStage(code);
        if (stage == null) {
            dao.createClassifierProcessingStage(code);
            return ClassifierProcessingStage.NONE;
        }
        return stage;
    }

    @Transactional
    public boolean setClassifierProcessingStageAtomically(String code, ClassifierProcessingStage expectedStage, ClassifierProcessingStage stage, Executable exec) {
        boolean lock = dao.lockStage(code);
        if (!lock)
            return false;
        ClassifierProcessingStage actual = getClassifierProcessingStage(code);
        if (expectedStage != null && expectedStage != actual)
            return false;
        dao.setClassifierProcessingStage(code, stage);
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RdmException(e);
        }
        return true;
    }

    @Transactional
    public void cleanClassifierSyncHistory(String code) {
        setClassifierRevisionAndLastUpdated(code, null, null);
    }

    @Transactional
    public GetClassifierStructureResponseType getClassifierStruct(String code, int revision) {
        String struct = dao.getClassifierStruct(code, revision);
        try {
            return ((CnsiResponse) STRUCT_CTX.createUnmarshaller().unmarshal(new StringReader(struct))).getGetClassifierStructure();
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
    }

    @Transactional
    public void insert(Map<String, String>[] batch, String tableName, String pageProcessorId) {
        int n = 0;
        for (int i = 0; i < batch.length && batch[i] != null; i++, n++);
        if (n != batch.length) {
            Map<String, String>[] cpy = new Map[n];
            System.arraycopy(batch, 0, cpy, 0, n);
            batch = cpy;
        }
        boolean finished = dao.isPageProcessorFinished(pageProcessorId);
        if (!finished) {
            dao.insertClassifierData(batch, tableName);
            dao.setPageProcessorFinished(pageProcessorId, true);
            dao.incrementPageProcessorSeed(pageProcessorId);
        }
    }

    @Transactional
    public Collection<PageProcessor> getIdleClassifierPageProcessor(String code, int revision) {
        String wildcard = getClassifierIdentifier(code, revision) + "-%";
        return dao.getFinishedPageProcessors(wildcard);
    }

    @Transactional
    public void setPageProcessorBusyAtomically(String pageProcessorId, Executable exec) {
        dao.setPageProcessorFinished(pageProcessorId, false);
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RdmException(e);
        }
    }

    @Transactional
    public void createEsnsiVersionDataTableAndRemovePreviousIfNecessaryAndSaveStruct(GetClassifierStructureResponseType struct) {
        String code = struct.getClassifierDescriptor().getCode();
        int revision = struct.getClassifierDescriptor().getRevision();
        String publicId = struct.getClassifierDescriptor().getPublicId();
        dao.dropClassifierDataTablesByWildcard(publicId + "-%");
        String tableName = getClassifierIdentifier(publicId, revision);
        dao.createClassifierRevisionDataTable(tableName, struct.getAttributeList().size());
        int primaryKeySerialNumber = -1;
        for (ClassifierAttribute attribute : struct.getAttributeList()) {
            if (attribute.isKey()) {
                primaryKeySerialNumber = attribute.getOrder() + 1;
                break;
            }
        }
        if (primaryKeySerialNumber == -1)
            throw new RdmException("Can't process classifier without primary key.");
        dao.createIndexOnClassifierRevisionDataTable(tableName, primaryKeySerialNumber);
        for (int i = 0; i < struct.getAttributeList().size(); i++)
            (FIELD_PREFIX + (i + 1)).intern(); // Добавляем в пул строк JVM, чтобы не загружать сборщик мусора
        StringWriter stringWriter = new StringWriter();
        try {
            CnsiResponse cnsiResponse = OBJECT_FACTORY.createCnsiResponse();
            cnsiResponse.setGetClassifierStructure(struct);
            STRUCT_CTX.createMarshaller().marshal(cnsiResponse, stringWriter);
        } catch (JAXBException e) {
//          Не выбросится
            throw new RdmException(e);
        }
        dao.saveClassifierRevisionStruct(code, revision, stringWriter.toString());
    }

    @Transactional
    public void setClassifierRevisionAndLastUpdated(String classifierCode, Integer revision, Timestamp timestamp) {
        dao.setClassifierRevisionAndLastUpdatedTimestamp(classifierCode, revision, timestamp);
        dao.setClassifierProcessingStage(classifierCode, ClassifierProcessingStage.NONE);
    }

    @Transactional
    public List<Map<String, Object>> getClassifierData(String publicId, int revision, String lastSeenId, int primaryKeySerialNumber) {
        return dao.getClassifierData(getClassifierIdentifier(publicId, revision), primaryKeySerialNumber, lastSeenId, PAGE_SIZE);
    }

    public static String getClassifierIdentifier(String id, int revision) {
        return id + "-" + revision;
    }

    @Transactional
    public void createPageProcessorStateRecords(String classifierCode, int revision, int numPageProcessors) {
        String pattern = getClassifierIdentifier(classifierCode, revision) + "-";
        dao.createPageProcessorStateRecordsResetToDefaultsOnConflict(pattern, numPageProcessors);
    }

}
