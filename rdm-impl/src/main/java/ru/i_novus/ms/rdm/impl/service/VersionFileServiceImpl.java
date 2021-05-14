package ru.i_novus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.*;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.impl.strategy.file.GenerateFileNameStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Primary
@Service
public class VersionFileServiceImpl implements VersionFileService {

    private final RefBookVersionRepository versionRepository;
    private final VersionFileRepository versionFileRepository;

    private final FileStorage fileStorage;

    private final PerRowFileGeneratorFactory fileGeneratorFactory;
    private final PassportValueRepository passportValueRepository;

    private final StrategyLocator fileNameStrategyLocator;

    private String passportFileHead = "fullName";
    private boolean includePassport = false;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionFileServiceImpl(RefBookVersionRepository versionRepository,
                                  VersionFileRepository versionFileRepository,
                                  FileStorage fileStorage,
                                  PerRowFileGeneratorFactory fileGeneratorFactory,
                                  PassportValueRepository passportValueRepository,
                                  StrategyLocator fileNameStrategyLocator) {
        this.versionRepository = versionRepository;
        this.versionFileRepository = versionFileRepository;

        this.fileStorage = fileStorage;

        this.fileGeneratorFactory = fileGeneratorFactory;
        this.passportValueRepository = passportValueRepository;

        this.fileNameStrategyLocator = fileNameStrategyLocator;
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
    public InputStream generate(RefBookVersion version, FileType fileType, Iterator<Row> rowIterator) {

        try (FileGenerator fileGenerator = fileGeneratorFactory
                .getFileGenerator(rowIterator, version, fileType);
             Archiver archiver = new Archiver()) {

            if (includePassport) {
                try (FileGenerator passportFileGenerator =
                             new PassportPdfFileGenerator(passportValueRepository, version, passportFileHead)) {
                    String passportFileName = getStrategy(version, GenerateFileNameStrategy.class)
                            .generateName(version, FileType.PDF);
                    archiver.addEntry(passportFileGenerator, passportFileName);
                }
            }

            String fileName = getStrategy(version, GenerateFileNameStrategy.class)
                    .generateName(version, fileType);

            return archiver
                    .addEntry(fileGenerator, fileName)
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

            RefBookVersionEntity versionEntity = versionRepository.getOne(version.getId());

            String zipFileName = getStrategy(version, GenerateFileNameStrategy.class)
                    .generateZipName(version, fileType);
            VersionFileEntity versionFileEntity = new VersionFileEntity(versionEntity, fileType,
                    fileStorage.saveContent(inputStream, zipFileName)
            );

            versionFileRepository.save(versionFileEntity);

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }

    private <T extends Strategy> T getStrategy(RefBookVersion version, Class<T> strategy) {

        return fileNameStrategyLocator.getStrategy(version != null ? version.getType() : null, strategy);
    }
}