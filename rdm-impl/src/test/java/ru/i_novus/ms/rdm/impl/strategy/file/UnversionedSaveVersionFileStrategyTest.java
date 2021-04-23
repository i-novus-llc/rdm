package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedSaveVersionFileStrategyTest {

    @InjectMocks
    private UnversionedSaveVersionFileStrategy unversionedFileVersionStrategy;

    private static final FileType TYPE_XML = FileType.XML;

    @Mock
    private VersionFileRepository versionFileRepository;

    @Test
    public void testUnversionedFileNotSaved() {

        RefBookVersion version = getRefBookVersion();

        VersionFileEntity versionFileEntity = getVersionFileEntity();

        when(versionFileRepository.findByVersionIdAndType(version.getId(), TYPE_XML))
                .thenReturn(versionFileEntity);

        unversionedFileVersionStrategy.save(version, TYPE_XML, "/");

        verify(versionFileRepository, times(0)).save(argThat(entity ->
                entity.getVersion().getId().equals(51) && entity.getType().equals(TYPE_XML)
        ));
    }

    private RefBookVersion getRefBookVersion() {

        RefBookVersion refBookVersion = new RefBookVersion();
        refBookVersion.setId(51);
        refBookVersion.setStatus(RefBookVersionStatus.DRAFT);

        return refBookVersion;
    }

    private VersionFileEntity getVersionFileEntity() {

        VersionFileEntity entity = new VersionFileEntity();
        entity.setId(12);
        entity.setType(FileType.XML);

        return entity;
    }
}