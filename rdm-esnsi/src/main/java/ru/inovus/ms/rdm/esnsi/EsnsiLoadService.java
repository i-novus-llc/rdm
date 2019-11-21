package ru.inovus.ms.rdm.esnsi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.esnsi.api.ClassifierAttribute;
import ru.inovus.ms.rdm.esnsi.api.CnsiResponse;
import ru.inovus.ms.rdm.esnsi.api.GetClassifierStructureResponseType;
import ru.inovus.ms.rdm.esnsi.api.ObjectFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
        return dao.getClassifierProcessingStage(code);
    }

    @Transactional
    public void setClassifierProcessingStageAtomically(String code, ClassifierProcessingStage stage, Executable exec) {
        dao.setClassifierProcessingStage(code, stage);
        try {
            exec.exec();
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new RdmException(e);
        }
    }

    @Transactional
    public ClassifierProcessingStage getClassifierProcessingStageAndCreateIfNotExists(String code) {
        ClassifierProcessingStage stage = dao.getClassifierProcessingStage(code);
        if (stage == null) {
            dao.createClassifierProcessingStageIfNotExists(code);
            return ClassifierProcessingStage.NONE;
        }
        return stage;
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
    public void insertAtomically(Map<String, String>[] batch, String code, int revision, String pageProcessorId, Executable exec) {
        int n = 0;
        for (int i = 0; i < batch.length && batch[i] != null; i++, n++);
        if (n != batch.length) {
            Map<String, String>[] cpy = new Map[n];
            System.arraycopy(batch, 0, cpy, 0, n);
            batch = cpy;
        }
        boolean finished = dao.isPageProcessorFinished(pageProcessorId);
        if (!finished) {
            dao.insertClassifierData(batch, getClassifierIdentifier(code, revision));
            dao.setPageProcessorFinished(pageProcessorId, true);
            try {
                exec.exec();
            } catch (Exception e) {
                logger.error(e.getMessage());
                throw new RdmException(e);
            }
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
        dao.dropClassifierDataTablesByWildcard(code + "-%");
        String tableName = getClassifierIdentifier(code, revision);
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
            ("field_" + (i + 1)).intern(); // Добавляем в пул строк JVM, чтобы не загружать сборщик мусора
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
    public void setClassifierRevisionAndLastUpdated(String classifierCode, int revision, Timestamp timestamp) {
        dao.setClassifierRevisionAndLastUpdatedTimestamp(classifierCode, revision, timestamp);
    }

    @Transactional
    public List<Map<String, Object>> getClassifierData(String classifierCode, int revision, String lastSeenId, int primaryKeySerialNumber) {
        return dao.getClassifierData(getClassifierIdentifier(classifierCode, revision), primaryKeySerialNumber, lastSeenId, PAGE_SIZE);
    }

    public static String getClassifierIdentifier(String code, int revision) {
        return code + "-" + revision;
    }

    @Transactional
    public void createPageProcessorStateRecords(String classifierCode, int revision, int numPageProcessors) {
        String pattern = getClassifierIdentifier(classifierCode, revision) + "-";
        dao.createPageProcessorStateRecordsResetToDefaultsOnConflict(pattern, numPageProcessors);
    }

}
