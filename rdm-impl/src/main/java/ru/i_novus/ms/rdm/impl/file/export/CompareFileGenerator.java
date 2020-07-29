package ru.i_novus.ms.rdm.impl.file.export;

import ru.i_novus.ms.rdm.api.model.ExportFile;


public interface CompareFileGenerator {

    ExportFile generateCompareFile(Integer oldVersionId, Integer newVersionId);
}
