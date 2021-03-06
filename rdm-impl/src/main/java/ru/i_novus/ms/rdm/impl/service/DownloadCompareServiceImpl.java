package ru.i_novus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.service.DownloadCompareService;
import ru.i_novus.ms.rdm.impl.file.export.CompareFileGenerator;
import ru.i_novus.ms.rdm.api.model.ExportFile;

/**
 * Created by znurgaliev on 25.09.2018.
 */
@Service
public class DownloadCompareServiceImpl implements DownloadCompareService {

    private CompareFileGenerator compareFileGenerator;

    @Autowired
    public DownloadCompareServiceImpl(CompareFileGenerator compareFileGenerator) {
        this.compareFileGenerator = compareFileGenerator;
    }

    @Override
    public ExportFile getCompareFile(Integer oldVersionId, Integer newVersionId) {
        return compareFileGenerator.generateCompareFile(oldVersionId, newVersionId);
    }
}
