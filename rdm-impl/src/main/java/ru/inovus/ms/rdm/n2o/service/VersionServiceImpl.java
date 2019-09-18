package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.entity.VersionFileEntity;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.n2o.model.ExportFile;
import ru.inovus.ms.rdm.n2o.model.Structure;
import ru.inovus.ms.rdm.n2o.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.RowValuePage;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.n2o.model.version.RefBookVersion;
import ru.inovus.ms.rdm.n2o.model.version.VersionCriteria;
import ru.inovus.ms.rdm.queryprovider.RefBookVersionQueryProvider;
import ru.inovus.ms.rdm.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.repository.VersionFileRepository;
import ru.inovus.ms.rdm.n2o.service.api.ExistsData;
import ru.inovus.ms.rdm.n2o.service.api.VersionFileService;
import ru.inovus.ms.rdm.n2o.service.api.VersionService;
import ru.inovus.ms.rdm.n2o.util.FileNameGenerator;
import ru.inovus.ms.rdm.n2o.util.ModelGenerator;
import ru.inovus.ms.rdm.n2o.util.TimeUtils;
import ru.inovus.ms.rdm.validation.ReferenceValidation;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

import static java.util.Collections.*;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;
import static ru.inovus.ms.rdm.n2o.util.ConverterUtil.*;
import static ru.inovus.ms.rdm.n2o.util.ModelGenerator.versionModel;

@Service
@Primary
public class VersionServiceImpl implements VersionService {

    private static final String ROW_NOT_FOUND_EXCEPTION_CODE = "row.not.found";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String ACTUAL_DATA_NOT_FOUND = "actual.data.not.found";

    private RefBookVersionRepository versionRepository;

    private SearchDataService searchDataService;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;

    private VersionFileRepository versionFileRepository;
    private VersionFileService versionFileService;

    @Autowired
    public VersionServiceImpl(RefBookVersionRepository versionRepository,
                              SearchDataService searchDataService,
                              FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                              VersionFileRepository versionFileRepository,
                              VersionFileService versionFileService) {
        this.versionRepository = versionRepository;

        this.searchDataService = searchDataService;

        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;

        this.versionFileRepository = versionFileRepository;
        this.versionFileService = versionFileService;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository
                .findById(versionId)
                .orElseThrow(() -> new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId)));
        return getRowValuesOfVersion(criteria, version);
    }

    /**
     * Получение списка версий справочника по параметрам критерия.
     *
     * @param criteria критерий поиска
     * @return Список версий справочника
     */
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
        RefBookVersionEntity version = versionRepository
                .findById(versionId)
                .orElseThrow(() -> new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId)));
        return versionModel(version);
    }

    @Override
    @Transactional
    public RefBookVersion getVersion(String version, String refBookCode) {
        RefBookVersionEntity versionEntity = versionRepository.findByVersionAndRefBookCode(version, refBookCode);
        if (versionEntity == null)
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, version));
        return versionModel(versionEntity);
    }

    @Override
    @Transactional
    public RefBookVersion getLastPublishedVersion(String refBookCode) {
        RefBookVersionEntity versionEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED);
        if (versionEntity == null)
            throw new NotFoundException(new Message(ReferenceValidation.LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE, refBookCode));
        return versionModel(versionEntity);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findActualOnDate(refBookCode, date);
        if (version == null) {
            throw new NotFoundException(new Message(ACTUAL_DATA_NOT_FOUND));
        }
        return getRowValuesOfVersion(criteria, version);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        return search(refBookCode, TimeUtils.now(), criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfVersion(SearchDataCriteria criteria, RefBookVersionEntity version) {
        List<Field> fields = fields(version.getStructure());
        Set<List<FieldSearchCriteria>> fieldSearchCriteriaList = new HashSet<>();
        fieldSearchCriteriaList.addAll(getFieldSearchCriteriaList(criteria.getAttributeFilter()));
        fieldSearchCriteriaList.addAll(getFieldSearchCriteriaList(criteria.getPlainAttributeFilter(), version.getStructure()));

        DataCriteria dataCriteria = new DataCriteria(version.getStorageCode(), version.getFromDate(), version.getToDate(),
                fields, fieldSearchCriteriaList, criteria.getRowSystemIds(), criteria.getCommonFilter());
        dataCriteria.setPage(criteria.getPageNumber() + 1);
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(sortings(sort)));

        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return pagedData.getCollection() != null
                ? new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, version.getId()))
                : null;
    }

    @Override
    @Transactional
    public Structure getStructure(Integer versionId) {
        return versionRepository.getOne(versionId).getStructure();
    }

    @Override
    @Transactional
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {
        if (fileType == null)
            return null;

        RefBookVersionEntity versionEntity = versionRepository
                .findById(versionId)
                .orElseThrow(() -> new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId)));

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        String path = null;
        if (fileEntity != null)
            path = fileEntity.getPath();

        if (fileEntity == null || !fileStorage.isExistContent(fileEntity.getPath())) {
            path = generateVersionFile(versionEntity, fileType);
        }

        return new ExportFile(
                fileStorage.getContent(path),
                fileNameGenerator.generateZipName(versionModel(versionEntity), fileType));
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
                if (hashes.containsKey(versionId))
                    hashes.get(versionId).add(split[0]);
                else hashes.put(versionId, new ArrayList<>(singleton(split[0])));
            }
        }

        for (Map.Entry<Integer, List<String>> entry : hashes.entrySet()) {
            RefBookVersionEntity versionEntity = versionRepository.getOne(entry.getKey());
            notExistent.addAll(searchDataService.getNotExists(
                    versionEntity.getStorageCode(),
                    versionEntity.getFromDate(),
                    versionEntity.getToDate(),
                    entry.getValue()));
        }
        return new ExistsData(notExistent.isEmpty(), notExistent);
    }

    @Override
    public RefBookRowValue getRow(String rowId) {

        if (!rowId.matches("^.+\\$\\d+$"))
            throw new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId));

        String[] split = rowId.split("\\$");
        RefBookVersionEntity version = versionRepository
                .findById(Integer.parseInt(split[1]))
                .orElseThrow(() -> new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId)));

        DataCriteria criteria = new DataCriteria(
                version.getStorageCode(),
                version.getFromDate(),
                version.getToDate(),
                fields(version.getStructure()),
                singletonList(split[0]));

        List<RowValue> data = searchDataService.getData(criteria);
        if (CollectionUtils.isEmpty(data))
            throw new NotFoundException(new Message(ROW_NOT_FOUND_EXCEPTION_CODE, rowId));

        if (data.size() > 1)
            throw new IllegalStateException("more than one row with id " + rowId);

        return new RefBookRowValue((LongRowValue) data.get(0), version.getId());
    }


    private String generateVersionFile(RefBookVersionEntity version, FileType fileType) {

        RefBookVersion versionModel = versionModel(version);

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
}
