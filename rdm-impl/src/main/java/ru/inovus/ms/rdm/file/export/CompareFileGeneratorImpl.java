package ru.inovus.ms.rdm.file.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.ExportFile;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.repositiory.PassportAttributeRepository;
import ru.inovus.ms.rdm.service.api.CompareService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.io.IOException;

@Component
public class CompareFileGeneratorImpl implements CompareFileGenerator {

    CompareService compareService;
    VersionService versionService;
    PassportAttributeRepository passportAttributeRepository;

    @Autowired
    public CompareFileGeneratorImpl(CompareService compareService, VersionService versionService, PassportAttributeRepository passportAttributeRepository) {
        this.compareService = compareService;
        this.versionService = versionService;
        this.passportAttributeRepository = passportAttributeRepository;
    }


    @Override
    @Transactional(readOnly = true)
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
