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
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.file.export.VersionDataIterator;

import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class DefaultExportDraftFileStrategyTest {

    private static final Integer VERSION_ID = 2;
    private static final FileType FILE_TYPE = FileType.XML;
    private static final String ZIP_NAME = "refBook_1.0.XML.zip";
    private static final String FILE_PATH = "/" + ZIP_NAME;

    @InjectMocks
    private DefaultExportDraftFileStrategy strategy;

    @Mock
    private VersionService versionService;

    @Mock
    private VersionFileService versionFileService;

    @Mock
    private FileNameGenerator fileNameGenerator;

    @Test
    public void testExport() {

        RefBookVersion version = createRefBookVersion();
        InputStream is = mock(InputStream.class);
        when(versionFileService.generate(eq(version), eq(FILE_TYPE), any(VersionDataIterator.class))).thenReturn(is);
        when(fileNameGenerator.generateZipName(eq(version), eq(FILE_TYPE))).thenReturn(ZIP_NAME);

        ExportFile expected = new ExportFile(is, ZIP_NAME);
        ExportFile actual = strategy.export(version, FILE_TYPE, versionService);
        assertEquals(expected, actual);

        verify(versionFileService).generate(eq(version), eq(FILE_TYPE), any(VersionDataIterator.class));
        verify(fileNameGenerator).generateZipName(version, FILE_TYPE);

        verifyNoMoreInteractions(versionFileService, fileNameGenerator);
    }

    private RefBookVersion createRefBookVersion() {

        RefBookVersion result = new RefBookVersion();
        result.setId(VERSION_ID);
        result.setStatus(RefBookVersionStatus.DRAFT);

        return result;
    }
}