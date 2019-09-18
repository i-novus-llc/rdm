package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.n2o.model.ExportFile;


public interface CompareFileGenerator {

    ExportFile generateCompareFile(Integer oldVersionId, Integer newVersionId);
}
