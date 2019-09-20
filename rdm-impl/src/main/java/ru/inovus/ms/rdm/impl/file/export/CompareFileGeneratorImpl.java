package ru.inovus.ms.rdm.impl.file.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.impl.repository.PassportAttributeRepository;
import ru.inovus.ms.rdm.api.service.CompareService;
import ru.inovus.ms.rdm.api.service.VersionService;

import java.io.IOException;

@Component
public class CompareFileGeneratorImpl implements CompareFileGenerator {

    private CompareService compareService;
    private VersionService versionService;
    private PassportAttributeRepository passportAttributeRepository;

    @Autowired
    @SuppressWarnings("unused")
    public CompareFileGeneratorImpl(CompareService compareService, VersionService versionService, PassportAttributeRepository passportAttributeRepository) {
        this.compareService = compareService;
        this.versionService = versionService;
        this.passportAttributeRepository = passportAttributeRepository;
    }


    @Override
    public ExportFile generateCompareFile(Integer oldVersionId, Integer newVersionId) {
        RefBookVersion oldVersion = versionService.getById(oldVersionId);
        RefBookVersion newVersion = versionService.getById(newVersionId);

        String filename = String.format("compare_result_%s_%s_%s",
                oldVersion.getVersion(),
                newVersion.getVersion(),
                oldVersion.getCode());

        try (FileGenerator diffGenerator = new XlsxCompareFileGenerator(oldVersionId, newVersionId, compareService, versionService, passportAttributeRepository);
             Archiver archiver = new Archiver()) {
            archiver.addEntry(diffGenerator, filename + ".xlsx");
            return new ExportFile(archiver.getArchive(), filename + ".zip");
        } catch (IOException e) {
            throw new RdmException("cannot get compare file", e);
        }
    }

}
