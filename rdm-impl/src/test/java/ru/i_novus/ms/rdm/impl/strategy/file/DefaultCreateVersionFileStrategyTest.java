package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DefaultCreateVersionFileStrategyTest {

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String ZIP_NAME = "refBook_1.0.XML.zip";
    private static final String FILE_PATH = "/" + ZIP_NAME;

    @InjectMocks
    private DefaultCreateVersionFileStrategy strategy;

    @Mock
    private VersionService versionService;

    @Mock
    private VersionFileService versionFileService;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private FileNameGenerator fileNameGenerator;

    @Test
    public void testCreate() {

        RefBookVersion version = createRefBookVersion();
        InputStream is = mock(InputStream.class);
        when(versionFileService.generate(eq(version), eq(FILE_TYPE), any(VersionDataIterator.class))).thenReturn(is);
        when(fileNameGenerator.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);
        when(fileStorage.saveContent(is, ZIP_NAME)).thenReturn(FILE_PATH);
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(Boolean.TRUE);

        String filePath = strategy.create(version, FILE_TYPE, versionService);
        assertEquals(FILE_PATH, filePath);
    }

    @Test
    public void testCreateWithoutContent() {

        RefBookVersion version = createRefBookVersion();
        InputStream is = mock(InputStream.class);
        when(versionFileService.generate(eq(version), eq(FILE_TYPE), any(VersionDataIterator.class))).thenReturn(is);
        when(fileNameGenerator.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);
        when(fileStorage.saveContent(is, ZIP_NAME)).thenReturn(FILE_PATH);
        when(fileStorage.isExistContent(FILE_PATH)).thenReturn(Boolean.FALSE);

        try {
            strategy.create(version, FILE_TYPE, versionService);
            fail("Create file without content");

        } catch (RuntimeException e) {
            assertEquals(RdmException.class, e.getClass());
        }
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setStatus(RefBookVersionStatus.PUBLISHED);

        return result;
    }
}