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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultFindVersionFileStrategyTest {

    private static final Integer VERSION_ID = 2;
    private static final Integer FILE_ID = 3;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String FILE_PATH = "/";

    @InjectMocks
    private DefaultFindVersionFileStrategy strategy;

    @Mock
    private VersionFileRepository versionFileRepository;

    @Test
    public void testFind() {

        VersionFileEntity versionFileEntity = createVersionFileEntity(VERSION_ID);
        versionFileEntity.setId(FILE_ID);

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE))
                .thenReturn(versionFileEntity);

        String filePath = strategy.find(VERSION_ID, FILE_TYPE);
        assertEquals(FILE_PATH, filePath);
    }

    @Test
    public void testFindNull() {

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE)).thenReturn(null);

        String filePath = strategy.find(VERSION_ID, FILE_TYPE);
        assertNull(filePath);
    }

    private RefBookVersion createRefBookVersion(RefBookVersionStatus status) {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setStatus(status);

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