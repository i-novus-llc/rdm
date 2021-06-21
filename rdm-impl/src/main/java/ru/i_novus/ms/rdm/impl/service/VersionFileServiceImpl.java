package ru.i_novus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.FileContentException;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.FileModel;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.*;
import ru.i_novus.ms.rdm.impl.file.process.FilePerRowProcessor;
import ru.i_novus.ms.rdm.impl.file.process.FileProcessorFactory;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.file.AllowStoreVersionFileStrategy;
import ru.i_novus.ms.rdm.impl.strategy.file.GenerateFileNameStrategy;
import ru.i_novus.ms.rdm.impl.util.FileUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

@Primary
@Service
public class VersionFileServiceImpl implements VersionFileService {

    private static final String FILE_CONTENT_INVALID_EXCEPTION_CODE = "file.content.invalid";

    private final RefBookVersionRepository versionRepository;
    private final VersionFileRepository versionFileRepository;

    private final FileStorage fileStorage;

    private final PerRowFileGeneratorFactory fileGeneratorFactory;
    private final PassportValueRepository passportValueRepository;

    private final StrategyLocator strategyLocator;

    private String passportFileHead = "fullName";
    private boolean includePassport = false;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionFileServiceImpl(RefBookVersionRepository versionRepository,
                                  VersionFileRepository versionFileRepository,
                                  FileStorage fileStorage,
                                  PerRowFileGeneratorFactory fileGeneratorFactory,
                                  PassportValueRepository passportValueRepository,
                                  StrategyLocator strategyLocator) {
        this.versionRepository = versionRepository;
        this.versionFileRepository = versionFileRepository;

        this.fileStorage = fileStorage;

        this.fileGeneratorFactory = fileGeneratorFactory;
        this.passportValueRepository = passportValueRepository;

        this.strategyLocator = strategyLocator;
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
    @Transactional
    public String create(RefBookVersion version, FileType fileType, VersionService versionService) {

        try (InputStream is = generate(version, fileType, versionService)) {

            return fileStorage.saveContent(is, generateZipName(version, fileType));

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private InputStream generate(RefBookVersion version, FileType fileType, VersionService versionService) {

        return generate(version, fileType,
                new VersionDataIterator(versionService, singletonList(version.getId()))
        );
    }

    @Override
    @Transactional
    public InputStream generate(RefBookVersion version, FileType fileType, Iterator<Row> rowIterator) {

        try (FileGenerator fileGenerator = fileGeneratorFactory.getFileGenerator(rowIterator, version, fileType);
             Archiver archiver = new Archiver()) {

            if (includePassport) {
                try (FileGenerator passportFileGenerator =
                             new PassportPdfFileGenerator(passportValueRepository, version, passportFileHead)) {
                    String passportFileName = getStrategy(version, GenerateFileNameStrategy.class)
                            .generateName(version, FileType.PDF);
                    archiver.addEntry(passportFileGenerator, passportFileName);
                }
            }

            String fileName = getStrategy(version, GenerateFileNameStrategy.class).generateName(version, fileType);
            return archiver.addEntry(fileGenerator, fileName)
                    .getArchive();

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    @Override
    @Transactional
    public void save(RefBookVersion version, FileType fileType, InputStream is) {

        try (InputStream inputStream = is) {
            if (inputStream == null) return;

            String filePath = fileStorage.saveContent(inputStream, generateZipName(version, fileType));
            insertEntity(version.getId(), fileType, filePath);

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    @Override
    public Supplier<InputStream> supply(String filePath) {

        return () -> fileStorage.getContent(filePath);
    }

    @Override
    @Transactional
    public ExportFile getFile(RefBookVersion version, FileType fileType, VersionService versionService) {
        
        boolean allowStore = getStrategy(version, AllowStoreVersionFileStrategy.class).allow(version);

        String filePath = allowStore ? findFilePath(version.getId(), fileType) : null;
        if (filePath == null) {
            filePath = createOrThrow(version, fileType, versionService);

            if (allowStore) {
                saveEntity(version.getId(), fileType, filePath);
            }
        }

        return buildExportFile(version, fileType, filePath);
    }

    private String findFilePath(Integer versionId, FileType fileType) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);
        String filePath = (fileEntity != null) ? fileEntity.getPath() : null;

        return (filePath != null && fileStorage.isExistContent(filePath)) ? filePath : null;
    }

    private String createOrThrow(RefBookVersion version, FileType fileType, VersionService versionService) {

        String filePath = create(version, fileType, versionService);

        if (filePath == null || !fileStorage.isExistContent(filePath))
            throw new RdmException("Cannot generate file");

        return filePath;
    }

    private void saveEntity(Integer versionId, FileType fileType, String filePath) {

        VersionFileEntity fileEntity = versionFileRepository.findByVersionIdAndType(versionId, fileType);

        if (fileEntity == null) {
            insertEntity(versionId, fileType, filePath);

        } else {
            updateEntity(fileEntity, filePath);
        }

    }

    private void insertEntity(Integer versionId, FileType fileType, String filePath) {

        RefBookVersionEntity versionEntity = versionRepository.getOne(versionId);
        VersionFileEntity createdEntity = new VersionFileEntity(versionEntity, fileType, filePath);
        versionFileRepository.save(createdEntity);
    }

    private void updateEntity(VersionFileEntity fileEntity, String filePath) {

        fileEntity.setPath(filePath);
        versionFileRepository.save(fileEntity);
    }

    private ExportFile buildExportFile(RefBookVersion version, FileType fileType, String filePath) {

        return new ExportFile(fileStorage.getContent(filePath), generateZipName(version, fileType));
    }

    @Override
    public void processRows(FileModel fileModel, RowsProcessor rowsProcessor, RowMapper rowMapper) {

        String extension = FileUtil.getExtension(fileModel.getName());
        Supplier<InputStream> fileSupplier = supply(fileModel.getPath());

        try (FilePerRowProcessor processor = FileProcessorFactory.createProcessor(extension, rowsProcessor, rowMapper)) {
            processor.process(fileSupplier);

        } catch (NoSuchElementException e) {
            if (FILE_CONTENT_INVALID_EXCEPTION_CODE.equals(e.getMessage()))
                throw new FileContentException(e);

            throw e;

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private String generateZipName(RefBookVersion version, FileType fileType) {

        return getStrategy(version, GenerateFileNameStrategy.class).generateZipName(version, fileType);
    }

    private <T extends Strategy> T getStrategy(RefBookVersion version, Class<T> strategy) {

        return strategyLocator.getStrategy(version != null ? version.getType() : null, strategy);
    }
}