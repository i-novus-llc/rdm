package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;

@Component
public class UnversionedFileVersionStrategy extends DefaultFileVersionStrategy{

    @Override
    protected void saveNewVersion(VersionFileEntity fileEntity,
                                  RefBookVersion versionModel, FileType fileType, String path) {
        //Ничего не делаем, потому что неверсионная версия файла
    }
}
