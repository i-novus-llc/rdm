package ru.i_novus.ms.rdm.impl.file;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TempFileTest {

    private static final String TEMP_DIR = "java.io.tmpdir";

    static {
        System.setProperty("logging.level.ru.i_novus.ms.rdm.impl.file.TempFileTest","INFO");
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testTmpDir() {

        final String tmpDir = System.getProperty(TEMP_DIR);
        assertNotNull(tmpDir);

        testDirectory(TEMP_DIR, new File(tmpDir));
    }

    @Test
    public void testTempRoot() {

        final File tempRoot = tempFolder.getRoot();
        testDirectory("temp folder", tempRoot);
    }

    private void testDirectory(String name, File directory) {

        log.info("Directory: {}={}", name, directory);

        assertNotNull(directory);

        assertTrue(directory.exists());
        assertTrue(directory.isDirectory());
        assertTrue(directory.canRead());
        assertTrue(directory.canWrite());

        final File file = new File(directory, "testfile.ext");
        try {
            createFile(file);

        } finally {
            if (file.exists()) {
                deleteFile(file);
            }
        }
    }

    private void createFile(File file) {
        try {
            final boolean created = file.createNewFile();
            if (created)
                log.info("File '{}' created successfully", file);
            else
                log.info("File '{}' already exists", file);

        } catch (IOException e) {
            log.error("Error creating file '{}':\n{}", file, e.getMessage());
            throw new RuntimeException(e);

        }
    }

    private void deleteFile(File file) {

        final boolean deleted = file.delete();
        if (deleted)
            log.info("File '{}' deleted successfully", file);
        else
            log.info("File '{}' cannot be deleted", file);
    }
}
