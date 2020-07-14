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
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.Archiver;
import ru.i_novus.ms.rdm.impl.file.export.FileGenerator;
import ru.i_novus.ms.rdm.impl.file.export.PassportPdfFileGenerator;
import ru.i_novus.ms.rdm.impl.file.export.PerRowFileGeneratorFactory;
import ru.i_novus.ms.rdm.impl.repository.PassportValueRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

@Primary
@Service
public class VersionFileServiceImpl implements VersionFileService {

    private RefBookVersionRepository versionRepository;
    private VersionFileRepository versionFileRepository;

    private FileStorage fileStorage;
    private FileNameGenerator fileNameGenerator;

    private PerRowFileGeneratorFactory fileGeneratorFactory;
    private PassportValueRepository passportValueRepository;

    private String passportFileHead = "fullName";
    private boolean includePassport = false;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionFileServiceImpl(RefBookVersionRepository versionRepository,
                                  VersionFileRepository versionFileRepository,
                                  FileStorage fileStorage,
                                  FileNameGenerator fileNameGenerator,
                                  PerRowFileGeneratorFactory fileGeneratorFactory,
                                  PassportValueRepository passportValueRepository) {
        this.versionRepository = versionRepository;
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
    public InputStream generate(RefBookVersion version, FileType fileType, Iterator<Row> rowIterator) {
        try (FileGenerator fileGenerator = fileGeneratorFactory
                .getFileGenerator(rowIterator, version, fileType);
             Archiver archiver = new Archiver()) {

            if (includePassport) {
                try (FileGenerator passportFileGenerator =
                             new PassportPdfFileGenerator(passportValueRepository, version, passportFileHead)) {
                    archiver.addEntry(passportFileGenerator, fileNameGenerator.generateName(version, FileType.PDF));
                }
            }

            return archiver
                    .addEntry(fileGenerator, fileNameGenerator.generateName(version, fileType))
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

            versionFileRepository.save(new VersionFileEntity(versionEntity, fileType,
                    fileStorage.saveContent(inputStream, fileNameGenerator.generateZipName(version, fileType))));

        } catch (IOException e) {
            throw new RdmException(e);
        }
    }
}