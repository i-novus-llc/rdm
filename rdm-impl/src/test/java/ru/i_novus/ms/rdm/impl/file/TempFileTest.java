package ru.i_novus.ms.rdm.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TempFileTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testTmpDir() {

        final String tmpDir = System.getProperty("java.io.tmpdir");
        assertNotNull(tmpDir);

        testDirectory(new File(tmpDir));
    }

    @Test
    public void testTempRoot() {

        final File tempRoot = tempFolder.getRoot();
        testDirectory(tempRoot);
    }

    private void testDirectory(File directory) {

        log.info("Directory: {}", directory);

        assertNotNull(directory);

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertTrue(directory.canRead());
        assertTrue(directory.canWrite());
    }
}
