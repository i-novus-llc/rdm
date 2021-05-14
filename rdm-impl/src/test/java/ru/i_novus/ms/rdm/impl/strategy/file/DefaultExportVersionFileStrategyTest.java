package ru.i_novus.ms.rdm.impl.strategy.file;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.impl.file.FileStorage;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultExportVersionFileStrategyTest {

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String ZIP_NAME = "refBook_1.0.XML.zip";
    private static final String FILE_PATH = "/" + ZIP_NAME;

    @InjectMocks
    private DefaultExportVersionFileStrategy strategy;

    @Mock
    private FileStorage fileStorage;

    @Mock
    private GenerateFileNameStrategy generateFileNameStrategy;

    @Test
    public void testExport() {
        
        RefBookVersion version = createRefBookVersion();
        InputStream is = mock(InputStream.class);
        when(fileStorage.getContent(FILE_PATH)).thenReturn(is);
        when(generateFileNameStrategy.generateZipName(version, FILE_TYPE)).thenReturn(ZIP_NAME);

        ExportFile expected = new ExportFile(is, ZIP_NAME);
        ExportFile actual = strategy.export(version, FILE_TYPE, FILE_PATH);
        assertEquals(expected, actual);

        verify(fileStorage).getContent(FILE_PATH);
        verify(generateFileNameStrategy).generateZipName(version, FILE_TYPE);

        verifyNoMoreInteractions(fileStorage, generateFileNameStrategy);
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setStatus(RefBookVersionStatus.PUBLISHED);

        return result;
    }
}