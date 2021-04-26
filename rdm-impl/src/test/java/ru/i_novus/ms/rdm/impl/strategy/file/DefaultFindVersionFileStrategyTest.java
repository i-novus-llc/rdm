package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.entity.VersionFileEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

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

    @Mock
    private FileStorage fileStorage;

    @Test
    public void testFind() {

        VersionFileEntity versionFileEntity = createVersionFileEntity();
        versionFileEntity.setId(FILE_ID);

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE)).thenReturn(versionFileEntity);
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(Boolean.TRUE);

        String filePath = strategy.find(VERSION_ID, FILE_TYPE);
        assertEquals(FILE_PATH, filePath);

        verify(versionFileRepository).findByVersionIdAndType(VERSION_ID, FILE_TYPE);
        verify(fileStorage).isExistContent(FILE_PATH);

        verifyNoMoreInteractions(versionFileRepository, fileStorage);
    }

    @Test
    public void testFindNull() {

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE)).thenReturn(null);

        String filePath = strategy.find(VERSION_ID, FILE_TYPE);
        assertNull(filePath);

        verify(versionFileRepository).findByVersionIdAndType(VERSION_ID, FILE_TYPE);

        verifyNoMoreInteractions(versionFileRepository, fileStorage);
    }

    @Test
    public void testFindWithoutContent() {

        VersionFileEntity versionFileEntity = createVersionFileEntity();
        versionFileEntity.setId(FILE_ID);

        when(versionFileRepository.findByVersionIdAndType(VERSION_ID, FILE_TYPE)).thenReturn(versionFileEntity);
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(Boolean.FALSE);

        String filePath = strategy.find(VERSION_ID, FILE_TYPE);
        assertNull(filePath);

        verify(versionFileRepository).findByVersionIdAndType(VERSION_ID, FILE_TYPE);
        verify(fileStorage).isExistContent(FILE_PATH);

        verifyNoMoreInteractions(versionFileRepository, fileStorage);
    }

    private VersionFileEntity createVersionFileEntity() {

        VersionFileEntity result = new VersionFileEntity();
        result.setType(FILE_TYPE);
        result.setPath(FILE_PATH);

        RefBookVersionEntity versionEntity = new RefBookVersionEntity();
        versionEntity.setId(VERSION_ID);
        result.setVersion(versionEntity);

        return result;
    }
}