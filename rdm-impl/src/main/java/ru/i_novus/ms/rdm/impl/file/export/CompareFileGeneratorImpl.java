package ru.i_novus.ms.rdm.impl.file.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.repository.PassportAttributeRepository;

import java.io.IOException;

@Component
public class CompareFileGeneratorImpl implements CompareFileGenerator {

    private final CompareService compareService;
    private final VersionService versionService;
    private final PassportAttributeRepository passportAttributeRepository;

    @Autowired
    @SuppressWarnings("unused")
    public CompareFileGeneratorImpl(CompareService compareService,
                                    VersionService versionService,
                                    PassportAttributeRepository passportAttributeRepository) {
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

        try (FileGenerator diffGenerator = new XlsxCompareFileGenerator(oldVersionId, newVersionId,
                compareService, versionService, passportAttributeRepository);
             Archiver archiver = new Archiver()) {

            archiver.addEntry(diffGenerator, filename + ".xlsx");
            return new ExportFile(archiver.getArchive(), filename + ".zip");

        } catch (IOException e) {
            throw new RdmException("cannot get compare file", e);
        }
    }

}
