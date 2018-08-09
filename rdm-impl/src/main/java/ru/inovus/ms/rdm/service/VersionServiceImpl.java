package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.beans.factory.annotation.Autowired;
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
import ru.inovus.ms.rdm.file.export.Archiver;
import ru.inovus.ms.rdm.file.export.FileGenerator;
import ru.inovus.ms.rdm.file.export.PerRowFileGeneratorFactory;
import ru.inovus.ms.rdm.file.export.VersionDataIterator;
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

    @Override
    public Page<RowValue> search(Integer versionId, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findOne(versionId);
        return getRowValuesOfVersion(criteria, version);
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
                fileNameGenerator.generateZipName(ModelGenerator.versionModel(versionEntity), FileType.XLSX));
    }

    private String generateVersionFile(RefBookVersionEntity version, FileType fileType) {

        RefBookVersion versionModel = ModelGenerator.versionModel(version);

        VersionDataIterator dataIterator = new VersionDataIterator(this, Collections.singletonList(versionModel.getId()));
        String path;
        try (FileGenerator fileGenerator = PerRowFileGeneratorFactory
                .getFileGenerator(dataIterator, this.getStructure(versionModel.getId()), fileType);
             Archiver archiver = new Archiver();
             InputStream is = archiver
                     .addEntry(fileGenerator, fileNameGenerator.generateName(versionModel, fileType))
                     .getArchive()) {
            path = fileStorage.saveContent(is, fileNameGenerator.generateZipName(versionModel, fileType));

            VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionModel.getId(), fileType);
            if (fileEntity == null) {
                RefBookVersionEntity versionEntity = new RefBookVersionEntity();
                versionEntity.setId(versionModel.getId());
                fileEntity = new VersionFileEntity(versionEntity, fileType, path);
            }
            versionFileRepository.save(fileEntity);

            if (!fileStorage.isExistContent(path))
                throw new RdmException("cannot generate file");
        } catch (IOException e) {
            throw new RdmException(e);
        }
        return path;
    }
}
