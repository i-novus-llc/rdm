package ru.inovus.ms.rdm.impl.file.export;

import ru.inovus.ms.rdm.api.model.ExportFile;


public interface CompareFileGenerator {

    ExportFile generateCompareFile(Integer oldVersionId, Integer newVersionId);
}
