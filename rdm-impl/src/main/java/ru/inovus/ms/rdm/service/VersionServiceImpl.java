package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.PassportAttributeEntity;
import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.entity.VersionFileEntity;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.repositiory.VersionFileRepository;
import ru.inovus.ms.rdm.service.api.ExistsData;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.FileNameGenerator;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasVersionId;
import static ru.inovus.ms.rdm.util.ConverterUtil.*;
import static ru.inovus.ms.rdm.util.ModelGenerator.versionModel;

@Service
@Primary
public class VersionServiceImpl implements VersionService {

    public static final String ROW_NOT_FOUND = "row.not.found";

    private RefBookVersionRepository versionRepository;
    private SearchDataService searchDataService;
    private FileNameGenerator fileNameGenerator;
    private VersionFileRepository versionFileRepository;
    private FileStorage fileStorage;
    private PassportValueRepository passportValueRepository;


    private String passportFileHead;
    private boolean includePassport;

    @Autowired
    public VersionServiceImpl(RefBookVersionRepository versionRepository,
                              SearchDataService searchDataService,
                              FileNameGenerator fileNameGenerator, VersionFileRepository versionFileRepository,
                              FileStorage fileStorage, PassportValueRepository passportValueRepository) {
        this.versionRepository = versionRepository;
        this.searchDataService = searchDataService;
        this.fileNameGenerator = fileNameGenerator;
        this.versionFileRepository = versionFileRepository;
        this.fileStorage = fileStorage;
        this.passportValueRepository = passportValueRepository;
    }

    @Value("${rdm.download.passport.head}")
    public void setPassportFileHead(String passportFileHead) {
        this.passportFileHead = passportFileHead;
    }

    @Value("${rdm.download.passport-enable}")
    public void setIncludePassport(boolean includePassport) {
        this.includePassport = includePassport;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findOne(versionId);
        if (version == null)
            throw new NotFoundException(new Message("version.not.found", versionId));
        return getRowValuesOfVersion(criteria, version);
    }

    @Override
    @Transactional
    public RefBookVersion getById(Integer versionId) {
        RefBookVersionEntity versionEntity = versionRepository.findOne(versionId);
        if (versionEntity == null)
            throw new NotFoundException(new Message("version.not.found", versionId));
        return versionModel(versionEntity);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, OffsetDateTime date, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findActualOnDate(refBookCode, date.toLocalDateTime());
        return version != null ? getRowValuesOfVersion(criteria, version) : new PageImpl<>(Collections.emptyList());
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        return search(refBookCode, OffsetDateTime.now(), criteria);
    }

    private Page<RefBookRowValue> getRowValuesOfVersion(SearchDataCriteria criteria, RefBookVersionEntity version) {
        List<Field> fields = fields(version.getStructure());
        Date bdate = date(version.getFromDate());
        Date edate = date(version.getToDate());

        Set<List<FieldSearchCriteria>> fieldSearchCriteriaList = new HashSet<>();
        fieldSearchCriteriaList.addAll(getFieldSearchCriteriaList(criteria.getAttributeFilter()));
        fieldSearchCriteriaList.addAll(getFieldSearchCriteriaList(criteria.getPlainAttributeFilter(), version.getStructure()));

        DataCriteria dataCriteria = new DataCriteria(version.getStorageCode(), bdate, edate,
                fields, fieldSearchCriteriaList, criteria.getCommonFilter());
        dataCriteria.setPage(criteria.getPageNumber() + 1);
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(sortings(sort)));
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return pagedData.getCollection() != null ? new RowValuePage(pagedData).map(rv -> new RefBookRowValue((LongRowValue) rv, version.getId())) : null;
    }

    @Override
    public Structure getStructure(Integer versionId) {
        return versionRepository.findOne(versionId).getStructure();
    }

    @Override
    @Transactional
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {
        RefBookVersionEntity versionEntity = versionRepository.findOne(versionId);
        if (versionEntity == null || fileType == null) return null;

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
    public RefBookVersion updatePassport(RefBookUpdateRequest refBookUpdateRequest) {
        RefBookVersionEntity refBookVersionEntity = versionRepository.findOne(refBookUpdateRequest.getVersionId());
        if (refBookVersionEntity == null) return null;

        updateVersionFromPassport(refBookVersionEntity, refBookUpdateRequest.getPassport());
        return versionModel(refBookVersionEntity);
    }

    @Override
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
            RefBookVersionEntity versionEntity = versionRepository.findOne(entry.getKey());
            notExistent.addAll(searchDataService.getNotExists(
                    versionEntity.getStorageCode(),
                    date(versionEntity.getFromDate()),
                    date(versionEntity.getToDate()),
                    entry.getValue()));

        }

        return new ExistsData(notExistent.isEmpty(), notExistent);
    }

    @Override
    public RefBookRowValue getRow(String rowId) {
        if (!rowId.matches("^.+\\$\\d+$")) throw new NotFoundException(ROW_NOT_FOUND);

        String[] split = rowId.split("\\$");
        RefBookVersionEntity version = versionRepository.findOne(Integer.parseInt(split[1]));
        if (version == null) throw new NotFoundException(ROW_NOT_FOUND);

        DataCriteria criteria = new DataCriteria(
                version.getStorageCode(),
                date(version.getFromDate()),
                date(version.getToDate()),
                fields(version.getStructure()),
                singletonList(split[0]));

        List<RowValue> data = searchDataService.getData(criteria);
        if (CollectionUtils.isEmpty(data)) throw new NotFoundException(ROW_NOT_FOUND);
        if (data.size() > 1) throw new IllegalStateException("more than one row with id " + rowId);
        return new RefBookRowValue((LongRowValue) data.get(0), version.getId());
    }

    private void updateVersionFromPassport(RefBookVersionEntity versionEntity, Map<String, String> newPassport) {
        if (newPassport == null) return;

        List<PassportValueEntity> valuesToRemove = versionEntity
                .getPassportValues()
                .stream()
                .filter(passportValue ->
                        newPassport.get(passportValue.getAttribute().getCode()) == null
                ).collect(Collectors.toList());

        passportValueRepository.delete(valuesToRemove);

        versionEntity
                .getPassportValues()
                .removeAll(valuesToRemove);

        newPassport
                .entrySet()
                .stream()
                .filter(newPV -> !isEmpty(newPV.getValue()))
                .forEach(newPV -> {
                    PassportValueEntity oldPV = versionEntity
                            .getPassportValues()
                            .stream()
                            .filter(pv ->
                                    newPV.getKey().equals(pv.getAttribute().getCode())
                            )
                            .findFirst()
                            .orElse(null);

                    if (oldPV != null)
                        oldPV.setValue(newPV.getValue());
                    else
                        versionEntity
                                .getPassportValues()
                                .add(new PassportValueEntity(new PassportAttributeEntity(newPV.getKey()), newPV.getValue(), versionEntity));
                });
    }


    private String generateVersionFile(RefBookVersionEntity version, FileType fileType) {

        RefBookVersion versionModel = versionModel(version);

        String path = null;
        try (InputStream is = generateVersionFile(versionModel, fileType)) {
            path = fileStorage.saveContent(is, fileNameGenerator.generateZipName(versionModel, fileType));
        } catch (IOException e) {
            throw new RdmException(e);
        }
        if (path == null || !fileStorage.isExistContent(path))
            throw new RdmException("cannot generate file");
        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionModel.getId(), fileType);
        if (fileEntity == null) {
            RefBookVersionEntity versionEntity = new RefBookVersionEntity();
            versionEntity.setId(versionModel.getId());
            fileEntity = new VersionFileEntity(versionEntity, fileType, path);
        }
        versionFileRepository.save(fileEntity);

        return path;
    }

    private InputStream generateVersionFile(RefBookVersion versionModel, FileType fileType) {
        VersionDataIterator dataIterator = new VersionDataIterator(this, Collections.singletonList(versionModel.getId()));
        try (PerRowFileGenerator fileGenerator = PerRowFileGeneratorFactory
                .getFileGenerator(dataIterator, versionModel, fileType);
             Archiver archiver = new Archiver()) {
            if (includePassport) {
                try (FileGenerator passportPdfFileGenerator = new PassportPdfFileGenerator(passportValueRepository, versionModel.getId(), passportFileHead)) {
                    archiver.addEntry(passportPdfFileGenerator, fileNameGenerator.generateName(versionModel, FileType.PDF));
                }
            }
            return archiver
                    .addEntry(fileGenerator, fileNameGenerator.generateName(versionModel, fileType))
                    .getArchive();
        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

}
