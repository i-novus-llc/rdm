package ru.i_novus.ms.rdm.api.model;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.i_novus.ms.rdm.api.BaseTest;

import java.io.InputStream;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class ExportFileTest extends BaseTest {

    @Mock
    InputStream inputStream;

    @Test
    public void testClass() {

        ExportFile exportFile = new ExportFile(inputStream, "fileName");

        ExportFile sameFile = new ExportFile(exportFile.getInputStream(), exportFile.getFileName());
        assertObjects(Assert::assertEquals, exportFile, sameFile);
    }

    @Test
    public void testEmptyClass() {

        ExportFile emptyFile = new ExportFile();
        assertNotNull(emptyFile);
        assertSpecialEquals(emptyFile);
    }
}