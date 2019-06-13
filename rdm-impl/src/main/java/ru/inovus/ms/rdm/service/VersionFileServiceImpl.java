package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.entity.*;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.file.FileStorage;
import ru.inovus.ms.rdm.file.export.*;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.PassportValueRepository;
import ru.inovus.ms.rdm.repositiory.VersionFileRepository;
import ru.inovus.ms.rdm.service.api.VersionFileService;
import ru.inovus.ms.rdm.util.FileNameGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Primary
@Service
public class VersionFileServiceImpl implements VersionFileService {

    private VersionFileRepository versionFileRepository;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;

    private PerRowFileGeneratorFactory fileGeneratorFactory;
    private PassportValueRepository passportValueRepository;

    private String passportFileHead = "fullName";
    private boolean includePassport = false;

    @Autowired
    @SuppressWarnings("all")
    public VersionFileServiceImpl(VersionFileRepository versionFileRepository,
                                  FileStorage fileStorage,
                                  FileNameGenerator fileNameGenerator,
                                  PerRowFileGeneratorFactory fileGeneratorFactory,
                                  PassportValueRepository passportValueRepository) {
        this.versionFileRepository = versionFileRepository;

        this.fileStorage = fileStorage;
        this.fileNameGenerator = fileNameGenerator;

        this.fileGeneratorFactory = fileGeneratorFactory;
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
    @Transactional
    public InputStream generate(RefBookVersion versionModel, FileType fileType, Iterator<Row> rowIterator) {
        try (FileGenerator fileGenerator = fileGeneratorFactory
                .getFileGenerator(rowIterator, versionModel, fileType);
             Archiver archiver = new Archiver()) {

            if (includePassport) {
                try (FileGenerator passportFileGenerator =
                             new PassportPdfFileGenerator(passportValueRepository, versionModel.getId(),
                                     passportFileHead, versionModel.getCode())) {
                    archiver.addEntry(passportFileGenerator, fileNameGenerator.generateName(versionModel, FileType.PDF));
                }
            }

            return archiver
                    .addEntry(fileGenerator, fileNameGenerator.generateName(versionModel, fileType))
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

            RefBookVersionEntity versionEntity = new RefBookVersionEntity();
            versionEntity.setId(version.getId());

            versionFileRepository.save(new VersionFileEntity(versionEntity, fileType,
                    fileStorage.saveContent(inputStream, fileNameGenerator.generateZipName(version, fileType))));

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }
}