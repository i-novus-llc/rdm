package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.entity.VersionFileEntity;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.repositiory.VersionFileRepository;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.ConverterUtil;
import ru.inovus.ms.rdm.util.FileNameGenerator;
import ru.inovus.ms.rdm.util.ModelGenerator;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.inovus.ms.rdm.util.ConverterUtil.date;
import static ru.inovus.ms.rdm.util.ConverterUtil.sortings;

@Service
@Primary
public class VersionServiceImpl implements VersionService {

    private RefBookVersionRepository versionRepository;
    private SearchDataService searchDataService;
    private FileNameGenerator fileNameGenerator;
    private VersionFileRepository versionFileRepository;
    private FileStorage fileStorage;


    private String passportFileHead;
    private boolean includePassport;

    @Autowired
    public VersionServiceImpl(RefBookVersionRepository versionRepository, SearchDataService searchDataService,
                              FileNameGenerator fileNameGenerator, VersionFileRepository versionFileRepository,
                              FileStorage fileStorage) {
        this.versionRepository = versionRepository;
        this.searchDataService = searchDataService;
        this.fileNameGenerator = fileNameGenerator;
        this.versionFileRepository = versionFileRepository;
        this.fileStorage = fileStorage;
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
    public Page<RowValue> search(Integer versionId, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findOne(versionId);
        return getRowValuesOfVersion(criteria, version);
    }

    @Override
    @Transactional
    public RefBookVersion getById(Integer versionId) {
        return ModelGenerator.versionModel(versionRepository.findOne(versionId));
    }

    @Override
    public Page<RowValue> search(Integer refbookId, OffsetDateTime date, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findActualOnDate(refbookId, date.toLocalDateTime());
        return version != null ? getRowValuesOfVersion(criteria, version) : new PageImpl<>(Collections.emptyList());
    }

    private Page<RowValue> getRowValuesOfVersion(SearchDataCriteria criteria, RefBookVersionEntity version) {
        List<Field> fields = ConverterUtil.fields(version.getStructure());
        Date bdate = date(version.getFromDate());
        Date edate = date(version.getToDate());
        DataCriteria dataCriteria = new DataCriteria(version.getStorageCode(), bdate, edate,
                fields, ConverterUtil.getFieldSearchCriteriaList(criteria.getAttributeFilter()), criteria.getCommonFilter());
        dataCriteria.setPage(criteria.getPageNumber());
        dataCriteria.setSize(criteria.getPageSize());
        Optional.ofNullable(criteria.getSort()).ifPresent(sort -> dataCriteria.setSortings(sortings(sort)));
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return pagedData.getCollection() != null ? new RowValuePage(pagedData) : null;
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
                fileNameGenerator.generateZipName(ModelGenerator.versionModel(versionEntity), fileType));
    }

    private String generateVersionFile(RefBookVersionEntity version, FileType fileType) {

        RefBookVersion versionModel = ModelGenerator.versionModel(version);

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
                .getFileGenerator(dataIterator, this.getStructure(versionModel.getId()), fileType);
             Archiver archiver = new Archiver()) {
            if (includePassport) {
                try (FileGenerator passportPdfFileGenerator = new PassportPdfFileGenerator(this, versionModel.getId(), passportFileHead)) {
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
