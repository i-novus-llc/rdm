package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.model.ExportFile;


public interface CompareFileGenerator {

    ExportFile generateCompareFile(Integer oldVersionId, Integer newVersionId);
}
