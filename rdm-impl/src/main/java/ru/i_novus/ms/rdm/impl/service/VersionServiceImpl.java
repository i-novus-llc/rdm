package ru.i_novus.ms.rdm.impl.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.ExistsData;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.RowValuePage;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.util.ModelGenerator;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.BaseDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.StorageDataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.hasVersionId;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;

@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class VersionServiceImpl implements VersionService {

    private static final String VERSION_WITH_NUMBER_AND_CODE_NOT_FOUND_EXCEPTION_CODE = "version.with.number.and.code.not.found";
    private static final String VERSION_ACTUAL_ON_DATE_NOT_FOUND_EXCEPTION_CODE = "version.actual.on.date.not.found";
    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";

    private RefBookVersionRepository versionRepository;

    private SearchDataService searchDataService;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;

    private VersionFileRepository versionFileRepository;
    private VersionFileService versionFileService;

    private AuditLogService auditLogService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionServiceImpl(RefBookVersionRepository versionRepository,
                              SearchDataService searchDataService,
                              FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                              VersionFileRepository versionFileRepository, VersionFileService versionFileService,
                              AuditLogService auditLogService) {
        this.versionRepository = versionRepository;

        this.searchDataService = searchDataService;

        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;

        this.versionFileRepository = versionFileRepository;
        this.versionFileService = versionFileService;

        this.auditLogService = auditLogService;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {

        RefBookVersionEntity version = getVersionOrThrow(versionId);
        return getRowValuesOfVersion(version, criteria);
    }

    @Override
    @Transactional
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {

        Sort.Order orderByFromDate = new Sort.Order(Sort.Direction.DESC,
                RefBookVersionQueryProvider.REF_BOOK_FROM_DATE_SORT_PROPERTY,
                Sort.NullHandling.NULLS_FIRST);

        PageRequest pageRequest = PageRequest.of(criteria.getPageNumber(), criteria.getPageSize(), Sort.by(orderByFromDate));
        Page<RefBookVersionEntity> list = versionRepository.findAll(RefBookVersionQueryProvider.toVersionPredicate(criteria), pageRequest);
        return list.map(ModelGenerator::versionModel);
    }

    @Override
    @Transactional
    public RefBookVersion getById(Integer versionId) {

        RefBookVersionEntity version = getVersionOrThrow(versionId);
        return ModelGenerator.versionModel(version);
    }

    @Override
    @Transactional
    public RefBookVersion getVersion(String version, String refBookCode) {

        RefBookVersionEntity versionEntity = versionRepository.findByVersionAndRefBookCode(version, refBookCode);
        if (versionEntity == null)
            throw new NotFoundException(new Message(VERSION_WITH_NUMBER_AND_CODE_NOT_FOUND_EXCEPTION_CODE, version, refBookCode));

        return ModelGenerator.versionModel(versionEntity);
    }

    @Override
    @Transactional
    public RefBookVersion getLastPublishedVersion(String refBookCode) {

        RefBookVersionEntity versionEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED);
        if (versionEntity == null)
            throw new NotFoundException(new Message(VersionValidationImpl.LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE, refBookCode));

        return ModelGenerator.versionModel(versionEntity);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {

        RefBookVersionEntity version = versionRepository.findActualOnDate(refBookCode, date);
        if (version == null)
            throw new NotFoundException(new Message(VERSION_ACTUAL_ON_DATE_NOT_FOUND_EXCEPTION_CODE));

        return getRowValuesOfVersion(version, criteria);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        return search(refBookCode, TimeUtils.now(), criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfVersion(RefBookVersionEntity version, SearchDataCriteria criteria) {

        List<Field> fields = makeOutputFields(version, criteria.getLocaleCode());

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getAttributeFilters()));
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getPlainAttributeFilters(), version.getStructure()));

        String storageCode = toStorageCode(version, criteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(storageCode, version.getFromDate(), version.getToDate(),
                fields, fieldSearchCriterias, criteria.getCommonFilter());
        dataCriteria.setHashList(criteria.getRowHashList());
        dataCriteria.setSystemIds(criteria.getRowSystemIds());

        dataCriteria.setPage(criteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(ConverterUtil.sortings(sort)));

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, version.getId()));
    }

    @Override
    @Transactional
    public Structure getStructure(Integer versionId) {

        RefBookVersionEntity entity = getVersionOrThrow(versionId);
        return entity.getStructure();
    }

    @Override
    @Transactional
    public String getStorageCode(Integer versionId) {

        RefBookVersionEntity entity = getVersionOrThrow(versionId);
        return entity.getStorageCode();
    }

    @Override
    @Transactional
    public ExistsData existsData(List<String> rowIds) {

        List<String> notExistent = new ArrayList<>();
        Map<Integer, List<String>> hashes = new HashMap<>();

        for (String rowId : rowIds) {
            if (!rowId.matches("^.+\\$\\d+$")) {
                notExistent.add(rowId);
                continue;
            }
            String[] split = rowId.split("\\$");
            Integer versionId = Integer.parseInt(split[1]);
            if (!versionRepository.exists(hasVersionId(versionId))) {
                notExistent.add(rowId);
            } else {
                String hash = split[0];
                if (hashes.containsKey(versionId))
                    hashes.get(versionId).add(hash);
                else
                    hashes.put(versionId, new ArrayList<>(singleton(hash)));
            }
        }

        for (Map.Entry<Integer, List<String>> entry : hashes.entrySet()) {
            Integer versionId = entry.getKey();
            RefBookVersionEntity entity = versionRepository.getOne(versionId);

            List<String> versionHashes = new ArrayList<>(entry.getValue());
            List<String> existentHashes = searchDataService.findExistentHashes(entity.getStorageCode(),
                    entity.getFromDate(), entity.getToDate(), versionHashes);

            versionHashes.removeAll(existentHashes);
            notExistent.addAll(versionHashes.stream().map(hash -> hash + "$" + versionId).collect(toList()));
        }

        return new ExistsData(notExistent.isEmpty(), notExistent);
    }

    @Override
    public RefBookRowValue getRow(String rowId) {

        if (!rowId.matches("^.+\\$\\d+$"))
            throw new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId));

        String[] split = rowId.split("\\$");
        final Integer versionId = Integer.parseInt(split[1]);
        RefBookVersionEntity version = getVersionOrThrow(versionId);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                version.getStorageCode(),
                version.getFromDate(),
                version.getToDate(),
                ConverterUtil.fields(version.getStructure()));
        dataCriteria.setHashList(singletonList(split[0]));

        List<RowValue> data = searchDataService.getData(dataCriteria);
        if (isEmpty(data))
            throw new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId));

        if (data.size() > 1)
            throw new IllegalStateException("more than one row with id " + rowId);

        return new RefBookRowValue((LongRowValue) data.get(0), version.getId());
    }

    private RefBookVersionEntity getVersionOrThrow(Integer versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException(new Message(VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE, versionId)));
    }

    @Override
    @Transactional
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {

        if (fileType == null)
            return null;

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        String path = (fileEntity != null) ? fileEntity.getPath() : null;
        if (fileEntity == null || !fileStorage.isExistContent(fileEntity.getPath())) {
            path = generateVersionFile(versionEntity, fileType);
        }

        ExportFile exportFile = new ExportFile(
                fileStorage.getContent(path),
                fileNameGenerator.generateZipName(ModelGenerator.versionModel(versionEntity), fileType)
        );

        auditLogService.addAction(AuditAction.DOWNLOAD, () -> versionEntity);

        return exportFile;
    }

    private String generateVersionFile(RefBookVersionEntity version, FileType fileType) {

        RefBookVersion versionModel = ModelGenerator.versionModel(version);

        String path;
        try (InputStream is = generateVersionFile(versionModel, fileType)) {
            path = fileStorage.saveContent(is, fileNameGenerator.generateZipName(versionModel, fileType));

        } catch (IOException e) {
            throw new RdmException(e);
        }

        if (path == null || !fileStorage.isExistContent(path))
            throw new RdmException("cannot generate file");

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionModel.getId(), fileType);
        if (fileEntity == null && !versionModel.isDraft()) {
            RefBookVersionEntity versionEntity = new RefBookVersionEntity();
            versionEntity.setId(versionModel.getId());

            fileEntity = new VersionFileEntity(versionEntity, fileType, path);
            versionFileRepository.save(fileEntity);
        }

        return path;
    }

    private InputStream generateVersionFile(RefBookVersion versionModel, FileType fileType) {

        VersionDataIterator dataIterator = new VersionDataIterator(this, Collections.singletonList(versionModel.getId()));
        return versionFileService.generate(versionModel, fileType, dataIterator);
    }

    /**
     * Формирование списка полей, выводимых в результате запроса данных в хранилище версии.
     *
     * @param version    версия справочника
     * @param localeCode код локали
     * @return Список выводимых полей
     */
    @SuppressWarnings("UnusedParameter")
    protected List<Field> makeOutputFields(RefBookVersionEntity version, String localeCode) {

        return ConverterUtil.fields(version.getStructure());
    }

    /**
     * Преобразование кода хранилища с учётом локали.
     *
     * @param version  версия
     * @param criteria критерий поиска
     * @return Код хранилища с учётом локали
     */
    @SuppressWarnings("UnusedParameter")
    protected String toStorageCode(RefBookVersionEntity version, SearchDataCriteria criteria) {
        return version.getStorageCode();
    }
}
