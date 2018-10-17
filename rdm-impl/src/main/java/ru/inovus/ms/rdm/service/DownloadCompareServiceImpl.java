package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.file.export.CompareFileGenerator;
import ru.inovus.ms.rdm.model.ExportFile;
import ru.inovus.ms.rdm.service.api.DownloadCompareService;

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
