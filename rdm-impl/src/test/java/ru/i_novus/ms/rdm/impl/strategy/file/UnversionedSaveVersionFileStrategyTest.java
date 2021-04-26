package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UnversionedSaveVersionFileStrategyTest {

    private static final int VERSION_ID = 2;
    private static final int FILE_ID = 3;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String FILE_PATH = "/refBook_1.0.XML.zip";

    @InjectMocks
    private UnversionedSaveVersionFileStrategy strategy;

    @Mock
    private VersionFileRepository versionFileRepository;

    @Test
    public void testUnversionedFileNotSaved() {

        VersionFileEntity versionFileEntity = createVersionFileEntity(VERSION_ID);
        versionFileEntity.setId(FILE_ID);

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE)).thenReturn(versionFileEntity);

        RefBookVersion version = createRefBookVersion();
        strategy.save(version, FILE_TYPE, FILE_PATH);

        verify(versionFileRepository).findByVersionIdAndType(VERSION_ID, FILE_TYPE);

        verifyNoMoreInteractions(versionFileRepository);
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setStatus(RefBookVersionStatus.PUBLISHED);

        return result;
    }

    private VersionFileEntity createVersionFileEntity(Integer versionId) {

        VersionFileEntity result = new VersionFileEntity();
        result.setType(FILE_TYPE);
        result.setPath(FILE_PATH);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(versionId);
        result.setVersion(versionEntity);

        return result;
    }
}