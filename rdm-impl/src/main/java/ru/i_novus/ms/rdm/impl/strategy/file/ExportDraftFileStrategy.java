package ru.i_novus.ms.rdm.impl.strategy.file;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.strategy.Strategy;

public interface ExportDraftFileStrategy extends Strategy {

    ExportFile export(RefBookVersion version, FileType fileType, VersionService versionService);
}
