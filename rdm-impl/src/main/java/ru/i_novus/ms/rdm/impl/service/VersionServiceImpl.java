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
import ru.i_novus.ms.rdm.api.model.ExistsData;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.RowValuePage;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.impl.audit.AuditAction;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.queryprovider.RefBookVersionQueryProvider;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.file.*;
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

import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.hasVersionId;
import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.toFieldSearchCriterias;
import static ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE;

@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class VersionServiceImpl implements VersionService {

    private static final String VERSION_WITH_NUMBER_AND_CODE_NOT_FOUND_EXCEPTION_CODE = "version.with.number.and.code.not.found";
    private static final String VERSION_ACTUAL_ON_DATE_NOT_FOUND_EXCEPTION_CODE = "version.actual.on.date.not.found";
    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";

    private final RefBookVersionRepository versionRepository;

    private final SearchDataService searchDataService;

    private final AuditLogService auditLogService;

    private final StrategyLocator strategyLocator;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionServiceImpl(RefBookVersionRepository versionRepository,
                              SearchDataService searchDataService,
                              AuditLogService auditLogService,
                              StrategyLocator strategyLocator) {
        this.versionRepository = versionRepository;

        this.searchDataService = searchDataService;

        this.auditLogService = auditLogService;

        this.strategyLocator = strategyLocator;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {

        RefBookVersionEntity entity = findOrThrow(versionId);
        return getRowValuesOfVersion(entity, criteria);
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

        RefBookVersionEntity entity = findOrThrow(versionId);
        return ModelGenerator.versionModel(entity);
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

        RefBookVersionEntity entity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED);
        if (entity == null)
            throw new NotFoundException(new Message(VersionValidationImpl.LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE, refBookCode));

        return ModelGenerator.versionModel(entity);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {

        RefBookVersionEntity entity = versionRepository.findActualOnDate(refBookCode, date);
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_ACTUAL_ON_DATE_NOT_FOUND_EXCEPTION_CODE));

        return getRowValuesOfVersion(entity, criteria);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {

        return search(refBookCode, TimeUtils.now(), criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfVersion(RefBookVersionEntity entity, SearchDataCriteria criteria) {

        List<Field> fields = makeOutputFields(entity, criteria.getLocaleCode());

        Set<List<FieldSearchCriteria>> fieldSearchCriterias = new HashSet<>();
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getAttributeFilters()));
        fieldSearchCriterias.addAll(toFieldSearchCriterias(criteria.getPlainAttributeFilters(), entity.getStructure()));

        String storageCode = toStorageCode(entity, criteria);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(storageCode, entity.getFromDate(), entity.getToDate(),
                fields, fieldSearchCriterias, criteria.getCommonFilter());
        dataCriteria.setHashList(criteria.getRowHashList());
        dataCriteria.setSystemIds(criteria.getRowSystemIds());

        dataCriteria.setPage(criteria.getPageNumber() + BaseDataCriteria.PAGE_SHIFT);
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(ConverterUtil.sortings(sort)));

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, entity.getId()));
    }

    @Override
    @Transactional
    public Structure getStructure(Integer versionId) {

        return findOrThrow(versionId).getStructure();
    }

    @Override
    @Transactional
    public String getStorageCode(Integer versionId) {

        return findOrThrow(versionId).getStorageCode();
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
        RefBookVersionEntity entity = findOrThrow(versionId);

        StorageDataCriteria dataCriteria = new StorageDataCriteria(
                entity.getStorageCode(),
                entity.getFromDate(),
                entity.getToDate(),
                ConverterUtil.fields(entity.getStructure()));
        dataCriteria.setHashList(singletonList(split[0]));

        List<RowValue> data = searchDataService.getData(dataCriteria);
        if (isEmpty(data))
            throw new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId));

        if (data.size() > 1)
            throw new IllegalStateException("more than one row with id " + rowId);

        return new RefBookRowValue((LongRowValue) data.get(0), entity.getId());
    }

    @Override
    @Transactional
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {

        if (fileType == null)
            return null;

        RefBookVersionEntity entity = findOrThrow(versionId);
        RefBookVersion version = ModelGenerator.versionModel(entity);

        String filePath = getStrategy(entity, FindVersionFileStrategy.class).find(versionId, fileType);
        if (filePath == null) {
            filePath = getStrategy(entity, CreateVersionFileStrategy.class).create(version, fileType, this);
            getStrategy(entity, SaveVersionFileStrategy.class).save(version, fileType, filePath);
        }

        ExportFile exportFile = getStrategy(entity, ExportVersionFileStrategy.class)
                .export(version, fileType, filePath);

        auditLogService.addAction(AuditAction.DOWNLOAD, () -> entity);

        return exportFile;
    }

    private RefBookVersionEntity findOrThrow(Integer id) {

        RefBookVersionEntity entity = (id != null) ? versionRepository.findById(id).orElse(null) : null;
        if (entity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, id));

        return entity;
    }

    private <T extends Strategy> T getStrategy(RefBookVersionEntity entity, Class<T> strategy) {

        return strategyLocator.getStrategy(entity != null ? entity.getRefBook().getType() : null, strategy);
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
