package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface GetExportFileStrategy extends Strategy {

    ExportFile get(RefBookVersionEntity entity, FileType fileType, VersionService versionService);
}
